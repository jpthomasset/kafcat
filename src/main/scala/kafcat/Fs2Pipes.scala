package kafcat

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

import cats.effect.IO
import cats.effect.kernel.Clock
import fs2.{Pipe, Stream, text}
import fs2.io.file.{Files, Path}
import fs2.kafka.CommittableConsumerRecord
import fs2.timeseries.TimeStamped

object Fs2Pipes {

  case class NoMoreEventException(reason: String) extends Exception(reason)

  /**
   * Pipe to convert a Byte stream to string lines stream
   */
  def toLines: Pipe[IO, Byte, String] =
    _.through(text.utf8.decode)
      .through(text.lines)
      .filter(!_.trim().isEmpty())

  /**
   * Pipe to convert a Byte stream to a string stream
   */
  def utf8Decode: Pipe[IO, Byte, String] =
    _.through(text.utf8.decode)
      .filter(!_.trim().isEmpty())

  /**
   * Build a stream for the given file content
   *
   * @param filename
   *   the filename to open
   */
  def readFile(filename: String) =
    Files[IO].readAll(Path(filename))

  /**
   * Pipe to optionaly abort stream if a value is a Failure. Otherwise, failure are passed through.
   *
   * @param abort
   *   set to true to abort on failure
   * @return
   */
  def abortOnFailure[T](abort: Boolean): Pipe[IO, Try[T], Try[T]] =
    _.flatMap(_ match {
      case Success(t)          => Stream.emit(Success(t))
      case Failure(e) if abort => Stream.raiseError[IO](e)
      case v                   => Stream(v)
    })

  def printFailures[T]: Pipe[IO, Try[T], Try[T]] =
    _.evalTap(_ match {
      case Failure(e) => IO(Console.err.println(s"Error: $e"))
      case _          => IO.unit
    })

  /**
   * Remove failure from stream
   */
  def filterOutFailures[T]: Pipe[IO, Try[T], T] =
    _.flatMap(_ match {
      case Success(t) => Stream.emit(t)
      case Failure(_) => Stream.empty
    })

  /**
   * Pipe to take only a subset of stream
   *
   * @param take
   *   Number of element to take, set to None for all
   */
  def take[T](take: Option[Int]): Pipe[IO, T, T] = s =>
    take match
      case Some(n) => s.take(n)
      case _       => s

  /**
   * Pipe to skip N records of a stream
   *
   * @param skip
   *   Number of element to skip, set to None for all
   */
  def skip[T](skip: Option[Int]): Pipe[IO, T, T] = s =>
    skip match
      case Some(n) => s.drop(n)
      case _       => s

  def skipNullValues[K, V](
    skip: Boolean
  ): Pipe[IO, CommittableConsumerRecord[IO, K, V], CommittableConsumerRecord[IO, K, V]] = s =>
    if skip then s.filter(_.record.value != null)
    else s

  /**
   * Timeout when no more event after some time
   *
   * @param maybeTimeout
   * @return
   */
  def timeoutWhenNoEvent[T](maybeTimeout: Option[FiniteDuration]): Pipe[IO, T, T] = s =>
    maybeTimeout match {
      case Some(timeout) =>
        s.evalMap(cr => Clock[IO].realTime.map(TimeStamped(_, cr)))
          .timeoutOnPullTo(
            timeout,
            Stream.raiseError[IO](NoMoreEventException(s"No more event to process after $timeout"))
          )
          .map(_.value)
      case None          => s
    }

}
