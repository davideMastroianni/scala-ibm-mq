package it.dmastro.queue

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.effect.testing.scalatest.AsyncIOSpec
import com.dimafeng.testcontainers.GenericContainer.DockerImage
import com.dimafeng.testcontainers.{
  Container,
  ForAllTestContainer,
  GenericContainer,
  MongoDBContainer,
  MultipleContainers
}
import it.dmastro.queue.configuration.{AppConfiguration, Endpoint}
import it.dmastro.queue.configuration.AppConfiguration.AppConfig
import it.dmastro.queue.resources.mongo.MongoStore
import it.dmastro.queue.utils.Utils._
import jms4s.config.{DestinationName, QueueName}
import jms4s.{JmsAutoAcknowledgerConsumer, JmsProducer}
import jms4s.ibmmq.ibmMQ
import jms4s.ibmmq.ibmMQ.{ClientId, Endpoint => MQEndpoint}
import jms4s.jms.{JmsMessage, MessageFactory}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Document, MongoClient}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.language.postfixOps

class QueueAppIT
    extends AsyncFreeSpec
    with ForAllTestContainer
    with AsyncIOSpec
    with Matchers {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  private final val QUEUE_NAME_1: String = "OMS.FLOW_COMPARATOR_1"
  private final val QUEUE_NAME_2: String = "OMS.FLOW_COMPARATOR_2"

  val mongoDBContainer: MongoDBContainer = MongoDBContainer(
    DockerImageName.parse("mongo:4.4")
  )

  val ibmMQContainer: GenericContainer = GenericContainer(
    dockerImage = DockerImage(Left("ibmcom/mq:9.1.5.0-r2")),
    exposedPorts = Seq(1414),
    env = Map(
      "LICENSE" -> "accept",
      "MQ_QMGR_NAME" -> "QMLYOMS",
      "MQ_DEV" -> "false"
    ),
    classpathResourceMapping = Seq(
      ("definitions.mqsc", "/etc/mqm/definitions.mqsc", BindMode.READ_ONLY)
    ),
    waitStrategy =
      Wait.forLogMessage(".*The listener 'DEV.LISTENER.TCP' has started.*", 1)
  )

  override def container: Container =
    MultipleContainers(
      mongoDBContainer,
      ibmMQContainer
    )

  def testResources(
      appConfig: AppConfig
  ): Resource[IO, (MongoClient, JmsProducer[IO])] =
    for {
      mongoClient <- MongoStore.makeClient(appConfig.mongoConfig)
      jmsClient <- ibmMQ.makeJmsClient(
        appConfig.mqConfig.copy(clientId = ClientId("test-pusher"))
      )
      jmsProducer <- jmsClient.createProducer(1)
    } yield (mongoClient, jmsProducer)

  def consumersAndProducerResources(
      appConfig: AppConfig
  ): Resource[
    IO,
    (
        JmsAutoAcknowledgerConsumer[IO],
        JmsAutoAcknowledgerConsumer[IO],
        JmsProducer[IO]
    )
  ] =
    for {
      jmsClient <- ibmMQ.makeJmsClient(
        appConfig.mqConfig.copy(clientId = ClientId("test-pusher"))
      )
      jmsProducer <- jmsClient.createProducer(1)
      jmsConsumer1 <- jmsClient.createAutoAcknowledgerConsumer(
        QueueName("OMS.FLOW_COMPARATOR_1.ERR"),
        concurrencyLevel = 1,
        pollingInterval = 1.millis
      )
      jmsConsumer2 <- jmsClient.createAutoAcknowledgerConsumer(
        QueueName("OMS.FLOW_COMPARATOR_2.ERR"),
        concurrencyLevel = 1,
        pollingInterval = 1.millis
      )
    } yield (jmsConsumer1, jmsConsumer2, jmsProducer)

  "GIVEN new xml message from queue AND OrderNo is new WHEN read message THEN insert message" in {

    val inboundMessageId = "2405B075321450"
    val xmlMessage1 =
      s"""
       <Order DocumentType="0001" EnterpriseCode="BALMAIN" OrderNo="$inboundMessageId"></Order>
      """
    val xmlMessage2 =
      s"""
       <Order DocumentType="0002" EnterpriseCode="BALMAIN" OrderNo="$inboundMessageId"></Order>
      """

    for {
      configTemplate <- AppConfiguration.configure
      appConfig = configTemplate.copy(
        mqConfig = configTemplate.mqConfig
          .copy(endpoints =
            NonEmptyList.of(
              MQEndpoint(ibmMQContainer.host, ibmMQContainer.mappedPort(1414))
            )
          ),
        mongoConfig = configTemplate.mongoConfig
          .copy(endpoints =
            List(
              Endpoint(
                mongoDBContainer.host,
                mongoDBContainer.mappedPort(27017)
              )
            )
          )
      )

      runningApp <- QueueApp.app(appConfig).start
      _ <- testResources(appConfig).use { case (mongoClient, jmsProducer) =>
        for {
          _ <- jmsProducer.send(make(xmlMessage1, QUEUE_NAME_1)) <* logger.info(
            s"\n\n\n ============ xml message 1 has been sent  ============ \n\n\n"
          )
          _ <- {
            val insertMessage = retryUntil(
              findInboundMessage(inboundMessageId, mongoClient)
            )(
              _.nonEmpty
            )
            insertMessage.asserting(_.size.shouldBe(1))
          }
          _ <- jmsProducer.send(make(xmlMessage2, QUEUE_NAME_2)) <* logger.info(
            s"\n\n\n ============ xml message 2 has been sent  ============ \n\n\n"
          )
          _ <- retryUntil(
            findInboundMessage(inboundMessageId, mongoClient)
          )(
            _.head
              .find(_._1 equals "messageList")
              .map(_._2.asArray().size())
              .contains(2)
          )
        } yield ()
      }
    } yield ()
  }

  def make(
      text: String,
      queueName: String
  ): MessageFactory[IO] => IO[(JmsMessage, DestinationName)] = { mFactory =>
    mFactory
      .makeTextMessage(text)
      .map(message => (message, QueueName(queueName)))
  }

  private def retryUntil[A](
      ioa: IO[A],
      delay: FiniteDuration = 1 seconds,
      maxRetries: Int = 10
  )(until: A => Boolean): IO[A] =
    ioa.flatMap(a =>
      if (until(a)) IO(a)
      else {
        if (maxRetries > 0)
          IO.sleep(delay) *> retryUntil(ioa, delay, maxRetries - 1)(until)
        else
          IO.raiseError(new RuntimeException(s"Exceeded max retries"))
      }
    )

  private def findInboundMessage(
      inboundMessageId: String,
      mongoClient: MongoClient
  ): IO[Seq[Document]] =
    mongoClient
      .getDatabase("oms-flow-comparator")
      .getCollection("inbound_message")
      .find(Filters.equal("inboundMessageId", inboundMessageId))
      .toIO

}
