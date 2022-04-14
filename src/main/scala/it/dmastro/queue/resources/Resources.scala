package it.dmastro.queue.resources

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.effect.kernel.Async
import it.dmastro.queue.configuration.AppConfiguration.AppConfig
import it.dmastro.queue.resources.jms.QueueConsumers
import jms4s.JmsTransactedConsumer
import org.typelevel.log4cats.Logger

object Resources {

  def from(appConfig: AppConfig)(implicit cs: Async[IO], logger: Logger[IO]): Resource[IO, Resources] =
    for {
      jmsConsumer <- QueueConsumers.make(appConfig.mqConfig, appConfig.queueNamesConfig)
    } yield Resources(jmsConsumer)

}

case class Resources(queueConsumer: NonEmptyList[JmsTransactedConsumer[IO]])

