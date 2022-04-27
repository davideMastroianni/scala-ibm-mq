package it.dmastro.queue

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import it.dmastro.queue.configuration.AppConfiguration
import it.dmastro.queue.configuration.AppConfiguration.AppConfig
import it.dmastro.queue.logic.{AbstractLogic, InboundMessageLogic}
import it.dmastro.queue.model.{NoOp, Success}
import it.dmastro.queue.resources.Resources
import jms4s.JmsTransactedConsumer.TransactionAction
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object QueueApp extends IOApp {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    AppConfiguration.configure
      .flatMap(appConfig =>
        app(appConfig)
          .onError(t => logger.error(t)("Error"))
      )
      .as(ExitCode.Success)

  def app(appConfig: AppConfig): IO[Unit] =
    Resources.from(appConfig).use {
      case Resources(queueConsumer, inboundMessageStore) =>
        val inboundMessageLogic: AbstractLogic =
          InboundMessageLogic.make(inboundMessageStore)

        queueConsumer.parTraverse_ {
          _.handle { case (msg, _) =>
            for {
              res <- inboundMessageLogic.describe(msg)
              action <- res.fold(
                t =>
                  logger
                    .error(t)("Rollbacking..." + msg.show)
                    .as(TransactionAction.rollback[IO]),
                {
                  case s: Success =>
                    logger.info(s.show).as(TransactionAction.commit[IO])
                  case n: NoOp =>
                    logger.debug(n.show).as(TransactionAction.commit[IO])
                }
              )
            } yield action
          }
        }
    }
}
