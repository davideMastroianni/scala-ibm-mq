package it.dmastro.queue.logic

import cats.effect.IO
import it.dmastro.queue.model.Result
import jms4s.jms.JmsMessage

trait AbstractLogic {

  def describe(message: JmsMessage): IO[Either[Throwable, Result]]

}
