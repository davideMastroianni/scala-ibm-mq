package it.dmastro.queue.resources.jms

import cats.data.NonEmptyList
import cats.effect.{Async, IO, Resource}
import cats.syntax.traverse._
import it.dmastro.queue.configuration.JmsQueueNameConfiguration.JmsQueueNameConfig
import jms4s.JmsTransactedConsumer
import jms4s.config.QueueName
import jms4s.ibmmq.ibmMQ
import jms4s.ibmmq.ibmMQ.{Config => JmsConfig}
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.DurationInt

object QueueConsumers {

  def make(
    jmsConfig: JmsConfig,
    queueName: JmsQueueNameConfig
  )(implicit cs: Async[IO], logger: Logger[IO]): Resource[IO, NonEmptyList[JmsTransactedConsumer[IO]]] = {
    for {
      jmsClient <- ibmMQ.makeJmsClient(jmsConfig)
      consumer  <- queueName.queueNames.map {
        queueName =>
          jmsClient.createTransactedConsumer(
            QueueName(queueName.value),
            concurrencyLevel = 5,
            pollingInterval = 100.millis
          )
      }.sequence
    } yield consumer

  }

}
