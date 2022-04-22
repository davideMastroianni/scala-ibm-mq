package it.dmastro.queue.configuration

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import it.dmastro.queue.configuration.JmsQueueNameConfiguration.JmsQueueNameConfig
import jms4s.ibmmq.ibmMQ.{Config => JmsConfig}

object AppConfiguration {

  case class AppConfig(
    mqConfig: JmsConfig,
    queueNamesConfig: JmsQueueNameConfig,
    mongoConfig: MongoDBConfiguration
  )

  def configure: IO[AppConfig] =
    for {
      config        <- IO(ConfigFactory.load())
      jms           <- JmsConfiguration.configure(config)
      jmsQueueNames <- JmsQueueNameConfiguration.configure(config)
      mongo         <- MongoDBConfiguration.configure(config)
    } yield AppConfig(jms, jmsQueueNames, mongo)
}
