/**
  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
  */
package actorbintree

import akka.actor._

import scala.collection.immutable.Queue

object BinaryTreeSet {

  trait Operation {
    def requester: ActorRef

    def id: Int

    def elem: Int
  }

  trait OperationReply {
    def id: Int
  }

  /** Request with identifier `id` to insert an element `elem` into the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Insert(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to check whether an element `elem` is present
    * in the tree. The actor at reference `requester` should be notified when
    * this operation is completed.
    */
  case class Contains(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to remove the element `elem` from the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Remove(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request to perform garbage collection */
  case object GC

  /** Holds the answer to the Contains request with identifier `id`.
    * `result` is true if and only if the element is present in the tree.
    */
  case class ContainsResult(id: Int, result: Boolean) extends OperationReply

  /** Message to signal successful completion of an insert or remove operation. */
  case class OperationFinished(id: Int) extends OperationReply

}


class BinaryTreeSet extends Actor {

  import BinaryTreeSet._
  import BinaryTreeNode._

  def createRoot: ActorRef = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))

  var root = createRoot

  // optional
  var pendingQueue = Queue.empty[Operation]

  // optional
  def receive = normal

  // optional
  /** Accepts `Operation` and `GC` messages. */
  val normal: Receive = {
    case insert: Insert =>
      root ! insert

    case contains: Contains =>
      root ! contains

    case Remove(requester, id, elem) =>
    // TODO

    case GC =>
    // TODO

    case e =>
      println(s"BinaryTreeSet ERROR!!! ${e}")
  }

  // optional
  /** Handles messages while garbage collection is performed.
    * `newRoot` is the root of the new binary tree where we want to copy
    * all non-removed elements into.
    */
  def garbageCollecting(newRoot: ActorRef): Receive = ???

}

object BinaryTreeNode {

  trait Position

  case object Left extends Position

  case object Right extends Position

  case class CopyTo(treeNode: ActorRef)

  case object CopyFinished

  def props(elem: Int, initiallyRemoved: Boolean) = Props(classOf[BinaryTreeNode], elem, initiallyRemoved)
}

class BinaryTreeNode(val elem: Int, initiallyRemoved: Boolean) extends Actor {

  import BinaryTreeNode._
  import BinaryTreeSet._

  var subtrees = Map[Position, ActorRef]()
  var removed = initiallyRemoved

  // optional
  def receive = normal

  // optional
  /** Handles `Operation` messages and `CopyTo` requests. */
  val normal: Receive = {
    case Insert(client, id, newElement) =>
      if (newElement == elem) {
        // nothing to do
        client ! OperationFinished(id) // TODO: the client? sure?
      } else if (newElement > elem) {
        // RIGHT
        subtrees.get(Right) match {
          case Some(child) =>
            child ! Insert(client, id, newElement)
          case None => {
            val nextChild: ActorRef = context.actorOf(props(newElement, true))
            subtrees += ((Right, nextChild))
            client ! OperationFinished(id)
          }
        }
      } else {
        // LEFT
        subtrees.get(Left) match {
          case Some(child) =>
            child ! Insert(client, id, newElement)
          case None => {
            val nextChild: ActorRef = context.actorOf(props(newElement, true))
            subtrees += ((Left, nextChild))
            client ! OperationFinished(id)
          }
        }
      }

    case Contains(requester, id, queryElement) =>
      if (elem == queryElement) {
        requester ! ContainsResult(id, result = true)
      } else if (queryElement > elem && (subtrees get Right).isDefined) {
        (subtrees get Right).foreach  { _ ! Contains(requester, id, queryElement)}
      } else if ((subtrees get Left).isDefined) {
        (subtrees get Left).foreach  { _ ! Contains(requester, id, queryElement)}
      } else {
        requester ! ContainsResult(id, result = false)
      }
    case _ =>
    // TODO
  }

  // optional
  /** `expected` is the set of ActorRefs whose replies we are waiting for,
    * `insertConfirmed` tracks whether the copy of this node to the new tree has been confirmed.
    */
  def copying(expected: Set[ActorRef], insertConfirmed: Boolean): Receive = ???


}