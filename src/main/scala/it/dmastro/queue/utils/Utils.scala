package it.dmastro.queue.utils

import cats.effect.IO
import org.mongodb.scala.{Observable, SingleObservable}

object Utils {

  implicit class ObservableOps[A](private val obs: Observable[A]) extends AnyVal {

    def toIO: IO[Seq[A]] =
      IO.fromFuture(IO(obs.toFuture()))

  }

  implicit class SingleObservableOps[A](private val obs: SingleObservable[A]) extends AnyVal {

    def toIO: IO[A] =
      IO.fromFuture(IO(obs.toFuture()))

  }
}
