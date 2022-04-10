package it.dmastro.queue.resources.jms

import cats.effect.{Async, IO, Resource}
import jms4s.JmsTransactedConsumer
import jms4s.config.QueueName
import jms4s.ibmmq.ibmMQ
import jms4s.ibmmq.ibmMQ.{Config => JmsConfig}
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.DurationInt

object QueueConsumer {

  def make(
    jmsConfig: JmsConfig
  )(implicit cs: Async[IO], logger: Logger[IO]): Resource[IO, JmsTransactedConsumer[IO]] =
    for {
      jmsClient <- ibmMQ.makeJmsClient(jmsConfig)
      consumer <- jmsClient.createTransactedConsumer(
        QueueName("DEV.QUEUE.1"),
        concurrencyLevel = 5,
        pollingInterval = 100.millis
      )
    } yield consumer

}
