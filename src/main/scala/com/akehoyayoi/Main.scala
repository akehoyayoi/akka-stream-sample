package com.akehoyayoi

import akka.stream.scaladsl._
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{OverflowStrategy, ThrottleMode}

import scala.concurrent.duration._
import scala.concurrent._
import scala.util.Random

object Main {

  def timedFuture[T](memo: String)(futureBlock: ⇒ Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val startTime: Long = System.nanoTime()
    val result: Future[T] = futureBlock
    result.onComplete(_ ⇒ println(memo + ":" + (System.nanoTime - startTime) / 1000000))
    result
  }

  def spin1(value: Int): Int = {
    val max = Random.nextInt(10)
    val start = System.currentTimeMillis()
    while ((System.currentTimeMillis() - start) < max) {}
    println("spin1 : " +  start)
    value
  }

  def spin2(value: Int): Int = {
    val max = Random.nextInt(20)
    val start = System.currentTimeMillis()
    while ((System.currentTimeMillis() - start) < max) {}
    println("spin2 : " +  start)
    value
  }

  implicit val system = ActorSystem("reactive-tweets")
  implicit val executionContext = system.dispatcher

  def main(args: Array[String]): Unit = {

    val max = 20

    val done1 = timedFuture("not async") {
      Source(1 to max)
      .map(spin1)
      .map(spin2)
      .runWith(Sink.ignore) }
    Await.result(done1,Duration.Inf)

    val done2 = timedFuture("applied async") {
      Source(1 to max)
      .map(spin1)
      .async
      .map(spin2)
      .runWith(Sink.ignore) }
    Await.result(done2,Duration.Inf)

    val done3 = timedFuture("mapAsync(parallelism = 1)") {
      Source(1 to max)
        .mapAsync(1)(x ⇒ Future(spin1(x)))
        .mapAsync(1)(x ⇒ Future(spin2(x)))
        .runWith(Sink.ignore)
    }
    Await.result(done3,Duration.Inf)

    val done4 = timedFuture("mapAsync(parallelism = 4)") {
      Source(1 to max)
        .mapAsync(4)(x => Future(spin1(x)))
        .mapAsync(4)(x => Future(spin2(x)))
        .runWith(Sink.ignore)
    }
    Await.result(done4,Duration.Inf)

    val done5 = timedFuture("mapAsync(parallelism = 1000)") {
      Source(1 to max)
        .mapAsync(1000)(x => Future(spin1(x)))
        .mapAsync(1000)(x => Future(spin2(x)))
        .runWith(Sink.ignore)
    }
    Await.result(done5,Duration.Inf)

    val done6 = timedFuture("mapAsyncUnordered(parallelism = 4)") {
      Source(1 to max)
        .mapAsyncUnordered(4)(x => Future(spin1(x)))
        .mapAsyncUnordered(4)(x => Future(spin2(x)))
        .runWith(Sink.ignore)
    }
    Await.result(done6,Duration.Inf)

    val done7 = timedFuture("use buffer") {
      Source(1 to max)
        .mapAsync(1)(x => Future(spin1(x)))
        .buffer(4, OverflowStrategy.backpressure)
        .mapAsync(100)(x => Future(spin2(x)))
        .runWith(Sink.ignore)
    }
    Await.result(done7,Duration.Inf)

    system.terminate()
  }
}
