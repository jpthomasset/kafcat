package kafcat

import scala.util.{Failure, Success, Try}

import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import fs2._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import fs2.kafka.CommittableConsumerRecord
import fs2.kafka.ConsumerRecord
import cats.effect.testkit._
import scala.concurrent.duration._
import kafcat.Fs2Pipes.NoMoreEventException

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

    "skip the number of line requested" in {
      val input = (1 to 10).map(n => s"content$n")
      Stream
        .emits(input)
        .through(Fs2Pipes.skip(Some(4)))
        .compile
        .count
        .asserting(_ shouldBe 6)

    }

    "skip null values" in {

      val cr1 = ConsumerRecord("topic", 0, 0, 1, "some event")
      val cr2 = ConsumerRecord("topic", 0, 0, 2, null)
      val cr3 = ConsumerRecord("topic", 0, 0, 3, "some event")

      Stream(cr1, cr2, cr3)
        .map(CommittableConsumerRecord(_, null))
        .through(Fs2Pipes.skipNullValues(true))
        .compile
        .count
        .asserting(_ shouldBe 2)

    }

    "keep null values" in {

      val cr1 = ConsumerRecord("topic", 0, 0, 1, "some event")
      val cr2 = ConsumerRecord("topic", 0, 0, 2, null)
      val cr3 = ConsumerRecord("topic", 0, 0, 3, "some event")

      Stream(cr1, cr2, cr3)
        .map(CommittableConsumerRecord(_, null))
        .through(Fs2Pipes.skipNullValues(false))
        .compile
        .count
        .asserting(_ shouldBe 3)

    }

    "timeout when no events" in {
      val input = List(1, 1, 1, 1, 6, 1, 1, 1)

      // Create a stream that sleeps incrementally more time
      val stream = Stream
        .emits(input)
        .covary[IO]
        .evalMap(i => IO.sleep(i.second) *> IO.pure(s"content$i"))
        .through(Fs2Pipes.timeoutWhenNoEvent(Some(5.seconds)))
        // Handle error by closing the stream
        .handleErrorWith { case NoMoreEventException(_) => Stream.empty }
        .compile
        .count

      TestControl
        .executeEmbed(stream)
        .asserting(_ shouldBe 4)
    }
  }
}
