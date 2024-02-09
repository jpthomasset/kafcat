package kafcat

import scala.util.{Failure, Success, Try}

import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import fs2._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class Fs2PipesSpec extends AsyncWordSpec with AsyncIOSpec with Matchers {
  "Fs2Pipes" should {

    "convert to lines" in {
      val input = "some content\nanother content"
      Stream
        .emits(input.getBytes())
        .through(Fs2Pipes.toLines)
        .compile
        .last
        .asserting(_ shouldBe Some("another content"))

    }

    "convert to string" in {
      val input = "some content\nanother content"
      Stream
        .emits(input.getBytes())
        .through(Fs2Pipes.utf8Decode)
        .compile
        .last
        .asserting(_ shouldBe Some(input))

    }

    "skip empty lines" in {
      val input = "some content\n\n   \nanother content"
      Stream
        .emits(input.getBytes())
        .through(Fs2Pipes.toLines)
        .compile
        .count
        .asserting(_ shouldBe 2)

    }

    "propagate failure" in {
      val input: List[Try[Int]] =
        List(Failure(new Exception("err")), Success(1))
      Stream
        .emits(input)
        .through(Fs2Pipes.abortOnFailure(false))
        .compile
        .count
        .asserting(_ shouldBe 2)
    }

    "abort on failure" in {
      val input: List[Try[Int]] =
        List(Failure(new Exception("err")), Success(1))
      Stream
        .emits(input)
        .through(Fs2Pipes.abortOnFailure(true))
        .compile
        .count
        .assertThrows[Exception]
    }

    "take only the number of line requested" in {
      val input = (1 to 10).map(n => s"content$n")
      Stream
        .emits(input)
        .through(Fs2Pipes.take(Some(4)))
        .compile
        .count
        .asserting(_ shouldBe 4)

    }

  }
}
