package com.akehoyayoi

import akka.stream.scaladsl._
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{OverflowStrategy, ThrottleMode}

import scala.concurrent.duration._
import scala.concurrent._
import scala.util.Random

object Main {

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

    println("not async")
    val done1 = Source(1 to max)
      .map(spin1)
      .map(spin2)
      .runWith(Sink.ignore)
    Await.result(done1,Duration.Inf)

    println("applied async")
    val done2 = Source(1 to max)
      .map(spin1)
      .async
      .map(spin2)
      .runWith(Sink.ignore)
    Await.result(done2,Duration.Inf)

    println("mapAsync(parallelism = 1)")
    val done3 = Source(1 to max)
      .mapAsync(1)(x ⇒ Future(spin1(x)))
      .mapAsync(1)(x ⇒ Future(spin2(x)))
      .runWith(Sink.ignore)
    Await.result(done3,Duration.Inf)

    println("mapAsync(parallelism = 4)")
    val done4 = Source(1 to max)
      .mapAsync(4)(x => Future(spin1(x)))
      .mapAsync(4)(x => Future(spin2(x)))
      .runWith(Sink.ignore)
    Await.result(done4,Duration.Inf)

    println("mapAsync(parallelism = 1000)")
    val done5 = Source(1 to max)
      .mapAsync(1000)(x => Future(spin1(x)))
      .mapAsync(1000)(x => Future(spin2(x)))
      .runWith(Sink.ignore)
    Await.result(done5,Duration.Inf)

    println("mapAsyncUnordered(parallelism = 4)")
    val done6 = Source(1 to max)
      .mapAsyncUnordered(4)(x => Future(spin1(x)))
      .mapAsyncUnordered(4)(x => Future(spin2(x)))
      .runWith(Sink.ignore)
    Await.result(done6,Duration.Inf)

    println("use buffer")
    val done7 = Source(1 to max)
      .mapAsync(1)(x => Future(spin1(x)))
      .buffer(4, OverflowStrategy.backpressure)
      .mapAsync(100)(x => Future(spin2(x)))
      .runWith(Sink.ignore)
    Await.result(done7,Duration.Inf)


    system.terminate()
  }
}
