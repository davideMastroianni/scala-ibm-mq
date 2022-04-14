package it.dmastro.queue.configuration

import cats.data.NonEmptyList
import cats.effect.IO
import com.typesafe.config.Config

import scala.jdk.CollectionConverters._

object JmsQueueNameConfiguration {

  case class JmsQueueNameConfig(
     queueNames: NonEmptyList[QueueNames]
   )

  case class QueueNames(value: String) extends AnyVal

  def configure(config: Config): IO[JmsQueueNameConfig] =
    IO {
      val queueNames = config
        .getStringList("jms.queueNames")
        .asScala
        .map(QueueNames)
        .toList

      JmsQueueNameConfig(
        queueNames = NonEmptyList.fromListUnsafe(queueNames)
      )
    }

}
