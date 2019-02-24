package downloader

import akka.actor.{Actor, Props}
import downloader.Receptionist.{Get, Result}

class Main extends Actor {
  val receptionist = context.actorOf(Props[Receptionist])

  receptionist ! Get("https://www.assimil.it")

  def receive = {
    case Result(url, links) =>
      println(s"Links found: ")
      println(links.mkString(", \n"))
      context.stop(self)
  }
}
