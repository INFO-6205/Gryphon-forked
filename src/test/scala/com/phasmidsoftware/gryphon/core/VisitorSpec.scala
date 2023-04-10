package com.phasmidsoftware.gryphon.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.collection.immutable.Queue

class VisitorSpec extends AnyFlatSpec with should.Matchers {

    behavior of "PostVisitor"

    it should "visitPost and ignore visitPre" in {
        val target: PostVisitor[Int, Queue[Int]] = PostVisitor()
        val t1 = target.visitPost(1)
        t1 shouldBe PostVisitor(Queue(1))
        val t2 = t1.visitPre(1)
        t2 shouldBe t1
        val t3 = t2.visitPost(2)
        t3 shouldBe PostVisitor(Queue(1, 2))
    }

    it should "implement create and appendable" in {
        val target = PostVisitor.create
        target.visitPre(1) shouldBe target
        val t1 = target.visitPost(1)
        t1.appendable shouldBe Seq(1)
        val t2 = t1.visitPost(2)
        t2.appendable shouldBe Queue(1, 2)
    }

    it should "implement reverse" in {
        val target = PostVisitor.reverse
        target.visitPre(1) shouldBe target
        val t1 = target.visitPost(1)
        t1 shouldBe PostVisitor(List(1))
        val t2 = t1.visitPost(2)
        t2 shouldBe PostVisitor(List(2, 1))
    }

    behavior of "PreVisitor"

    it should "visitPre and ignore visitPost" in {
        val target: PreVisitor[Int, Queue[Int]] = PreVisitor()
        val t2 = target.visitPre(1)
        t2 shouldBe PreVisitor(Queue(1))
        val t3 = t2.visitPre(2)
        t3 shouldBe PreVisitor(Queue(1, 2))
    }

    it should "implement create and appendable" in {
        val target = PreVisitor.create
        val t1 = target.visitPre(1)
        t1.appendable shouldBe Seq(1)
        val t2 = t1.visitPre(2)
        t2.appendable shouldBe Queue(1, 2)
    }

    it should "implement reverse" in {
        val target = PreVisitor.reverse
        val t1 = target.visitPre(1)
        t1 shouldBe PreVisitor(List(1))
        val t2 = t1.visitPre(2)
        t2 shouldBe PreVisitor(List(2, 1))
    }

    behavior of "Visitor"

    it should "preFunc" in {
        val target: PreVisitor[Int, Queue[Int]] = PreVisitor()
        val queue = Queue.empty[Int]
        val a1: Option[Queue[Int]] = target.preFunc(1)(queue)
        a1 shouldBe Some(Queue(1))
    }

//    it should "join" in {
//        val preVisitor: PreVisitor[Int] = PreVisitor()
//        val postVisitor: PostVisitor[Int] = PostVisitor()
//        val target: Visitor[Int, Queue[Int]] = preVisitor join postVisitor
//        val queue = Queue.empty[Int]
//        val z: Visitor[Int, Queue[Int]] = target.visitPre(queue)(1)
//        z shouldBe Queue.empty
//    }

    it should "postFunc" in {
        val target: PostVisitor[Int, Queue[Int]] = PostVisitor()
        val queue = Queue.empty[Int]
        val a1: Option[Queue[Int]] = target.postFunc(1)(queue)
        a1 shouldBe Some(Queue(1))
    }

}