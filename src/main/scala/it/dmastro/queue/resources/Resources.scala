package it.dmastro.queue.resources

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.effect.kernel.Async
import it.dmastro.queue.configuration.AppConfiguration.AppConfig
import it.dmastro.queue.resources.jms.QueueConsumers
import it.dmastro.queue.resources.mongo.MongoStore
import it.dmastro.queue.resources.mongo.collections.InboundMessageCollection
import jms4s.JmsTransactedConsumer
import org.typelevel.log4cats.Logger

object Resources {

  def from(appConfig: AppConfig)(implicit cs: Async[IO], logger: Logger[IO]): Resource[IO, Resources] =
    for {
      jmsConsumer <- QueueConsumers.make(appConfig.mqConfig, appConfig.queueNamesConfig)
      mongoClient <- MongoStore.makeClient(appConfig.mongoConfig)
      imStore     <- Resource.eval(MongoStore.resource(
        appConfig.mongoConfig, mongoClient, new InboundMessageCollection))
    } yield Resources(jmsConsumer, imStore)

}

case class Resources(queueConsumer: NonEmptyList[JmsTransactedConsumer[IO]],
                     inboundMessageStore: MongoStore)

