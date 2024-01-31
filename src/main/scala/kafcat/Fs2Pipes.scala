package kafcat

import scala.util.{Failure, Success, Try}

import cats.effect.IO
import fs2.{Pipe, Stream, text}
import fs2.io.file.{Files, Path}

object Fs2Pipes {

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

}
