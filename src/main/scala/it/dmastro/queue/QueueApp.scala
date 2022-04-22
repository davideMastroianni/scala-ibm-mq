package it.dmastro.queue

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import it.dmastro.queue.configuration.AppConfiguration
import it.dmastro.queue.configuration.AppConfiguration.AppConfig
import it.dmastro.queue.model.{NoOp, Result, Success}
import it.dmastro.queue.resources.Resources
import jms4s.JmsTransactedConsumer.TransactionAction
import jms4s.jms.JmsMessage
import org.mongodb.scala.Document
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

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
      case Resources(queueConsumer, inboundMessageStore) =>
        def logic(message: JmsMessage): IO[Either[Throwable, Result]] = {
          (for {
            insert <- inboundMessageStore.insert(
              Document(
                "_id" -> UUID.randomUUID().toString,
                "message" -> message.attemptAsText.get
              ))
          } yield Success(message, insert)).attempt
        }

        queueConsumer.parTraverse_ {
          _.handle {
            case (msg, _) =>
              for {
                res <- logic(msg)
                action <- res.fold(
                  t => logger.error(t)("Rollbacking..." + msg.show).as(TransactionAction.rollback[IO]), {
                    case s: Success => logger.info(s.show).as(TransactionAction.commit[IO])
                    case n: NoOp    => logger.debug(n.show).as(TransactionAction.commit[IO])
                  }
                )
              } yield action
          }
        }
    }
}
