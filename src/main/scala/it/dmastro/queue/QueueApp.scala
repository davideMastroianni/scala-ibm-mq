package it.dmastro.queue

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import it.dmastro.queue.configuration.AppConfiguration
import it.dmastro.queue.configuration.AppConfiguration.AppConfig
import it.dmastro.queue.resources.Resources
import jms4s.JmsTransactedConsumer.TransactionAction
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object QueueApp extends IOApp {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    AppConfiguration.configure
      .flatMap(
        appConfig => app(appConfig)
          .handleErrorWith(t => logger.error(t)("Error"))
      )
      .as(ExitCode.Success)

  def app(appConfig: AppConfig): IO[Unit] =
    Resources.from(appConfig).use {
      case Resources(queueConsumer) =>
        queueConsumer.parTraverse_ {
          _.handle {
            case (msg, _) => logger.info(s"message --> ${msg.attemptAsText.get}").as(TransactionAction.commit[IO])
          }
        }
    }
}
