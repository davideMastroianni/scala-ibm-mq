package it.dmastro.queue.configuration

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import jms4s.ibmmq.ibmMQ.{ Config => JmsConfig }

object AppConfiguration {

  case class AppConfig(mqConfig: JmsConfig)
  case class PoolSize(size: Int) extends AnyVal

  def configure: IO[AppConfig] =
    for {
      config  <- IO(ConfigFactory.load())
      jms     <- JmsConfiguration.configure(config)
    } yield AppConfig(jms)
}
