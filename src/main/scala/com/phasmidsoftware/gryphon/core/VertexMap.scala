package com.phasmidsoftware.gryphon.core

import com.phasmidsoftware.gryphon.core.Queueable.QueueableQueue
import com.phasmidsoftware.gryphon.core.VertexMap.findAndMarkVertex
import scala.annotation.tailrec
import scala.collection.immutable.{HashMap, Queue, TreeMap}

/**
 * Trait to define the behavior of a "vertex map," i.e. the set of adjacency lists for a graph.
 *
 * The adjacency list (of type AdjacencyList[X]) for a vertex (type V) points to edges of type X which, in turn, reference
 * vertices of type Vertex[V, X].
 *
 * There are two distinct types of VertexMap:
 * <ol>
 * <li>Those that can be ordered according to type V (these will use a TreeMap)</li>
 * <li>Those that can't be ordered according to type V (these will use a HashMap)</li>
 * </ol>
 *
 * @tparam V the (key) vertex-type of a graph.
 * @tparam X the edge-type of a graph. A sub-type of EdgeLike[V].
 */
trait VertexMap[V, +X <: EdgeLike[V], P] extends Traversable[V] {
    self =>

    /**
     * Method to determine if this VertexMap contains a vertex with attribute v.
     *
     * @param v the attribute of the vertex.
     * @return true if this VertexMap contains a vertex with attribute v.
     */
    def contains(v: V): Boolean

    /**
     * Method to determine if this VertexMap contains a vertex at the other end of the given edge from the vertex with attribute v.
     *
     * @param v the attribute of the vertex.
     * @param x (Y >: X <: EdgeLike[V]) an edge.
     * @return true if this VertexMap contains the vertex at the other end of x from v.
     */
    def containsOther[Y >: X <: EdgeLike[V]](v: V, x: Y): Boolean = contains(x.otherVertex(v))

    /**
     * Method to yield the number of adjacency lists defined in this VertexMap.
     *
     * @return the size of this VertexMap.
     */
    def size: Int

    /**
     * Method to get the AdjacencyList for vertex with key (attribute) v, if there is one.
     *
     * @param v the key (attribute) of the vertex whose adjacency list we require.
     * @return an Option of AdjacencyList[X].
     */
    def optAdjacencyList(v: V): Option[AdjacencyList[X]]

    /**
     * Method to get a sequence of the adjacent edges for vertex with key (attribute) v.
     *
     * @param v the key (attribute) of the vertex whose adjacency list we require.
     * @return <code>optAdjacencyList(v).toSeq.flatMap(_.xs)</code>.
     */
    def adjacentEdges(v: V): Seq[X] = optAdjacencyList(v).toSeq.flatMap(_.xs)

    /**
     * Method to get a sequence of the adjacent edges for vertex with key (attribute) v.\
     * that also satisfy the predicate given.
     *
     * @param v the key (attribute) of the vertex whose adjacency list we require.
     * @param p a predicate of type X => Boolean.
     * @return <code>adjacentEdges(v) filter p</code>.
     */
    def adjacentEdgesWithFilter(v: V)(p: X => Boolean): Seq[X] = adjacentEdges(v) filter p

    /**
     * the vertex-type values, i.e. the keys, of this VertexMap.
     */
    val keys: Iterable[V]

    /**
     * the Vertex[V, X] values of this VertexMap.
     */
    def values[Y >: X <: EdgeLike[V]]: Iterable[Vertex[V, Y, P]]

    /**
     * the X values of this VertexMap.
     */
    val edges: Iterable[X]

    /**
     * Method to add a vertex to this VertexMap.
     *
     * @param v the (key) value of the vertex to be added.
     * @return a new VertexMap which includes all the original entries of <code>this</code> plus <code>v</code>.
     */
    def addVertex(v: V): VertexMap[V, X, P]

    /**
     * Method to add an edge to this VertexMap.
     *
     * @param v the (key) value of the vertex whose adjacency list we are adding to.
     * @param y the edge to be added to the adjacency list.
     * @tparam Y a super-type of X.
     * @return a new VertexMap which includes all the original entries of <code>this</code> plus <code>v -> x</code>.
     */
    def addEdge[Y >: X <: EdgeLike[V]](v: V, y: Y): VertexMap[V, Y, P]
}

trait OrderedVertexMap[V, +X <: EdgeLike[V], P] extends VertexMap[V, X, P] {

    /**
     * This method adds an edge y (Y) to this OrderedVertexMap and returns
     * a tuple formed from the new vertex and the new VertexMap.
     * This is particularly used in Prim's algorithm (and maybe Dijkstra's algorithm, too).
     *
     * @param y the edge to be added.
     * @tparam Y the edge type.
     * @return a tuple as described above.
     */
    def addEdgeWithVertex[Y >: X <: EdgeLike[V]](y: Y): (Some[V], OrderedVertexMap[V, Y, P]) = {
        val (v1, v2) = y.vertices
        val (in, out) = if (contains(v1)) (v1, v2) else (v2, v1)
        Some(out) -> addVertex(out).addEdge(in, y).asInstanceOf[OrderedVertexMap[V, X, P]]
    }

}

/**
 * Case class to represent an ordered VertexMap.
 * that's to say a VertexMap where V is ordered, typically used for an ConcreteUndirectedGraph).
 * The ordering is based on the key (V) type.
 *
 * @param map a TreeMap of V -> Vertex[V, X].
 * @tparam V the (key) vertex-attribute type.
 *           Requires implicit evidence of type Ordering[V].
 * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
 */
case class OrderedVertexMapCase[V: Ordering, X <: EdgeLike[V], P: HasZero](map: TreeMap[V, Vertex[V, X, P]]) extends BaseVertexMap(map) with OrderedVertexMap[V, X, P] {

    /**
     * Method to construct a new OrderedVertexMapCase from the given map.
     *
     * @param map a TreeMap. If it is not a TreeMap, it will be converted to one.
     * @return a new OrderedVertexMapCase[V, X].
     */
    def unit[Y >: X <: EdgeLike[V]](map: Map[V, Vertex[V, Y, P]]): VertexMap[V, Y, P] = {
        val zz: TreeMap[V, Vertex[V, Y, P]] = map.to(TreeMap)
        OrderedVertexMapCase[V, Y, P](zz)
    }
}

/**
 * Companion object to OrderedVertexMapCase.
 */
object OrderedVertexMap {
    def apply[V: Ordering, X <: EdgeLike[V], P: HasZero](v: V): VertexMap[V, X, P] = empty[V, X, P].addVertex(v)

    /**
     * Method to yield an empty OrderedVertexMapCase.
     *
     * @tparam V the (key) vertex-attribute type.
     *           Requires implicit evidence of type Ordering[V].
     * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
     * @return an empty OrderedVertexMapCase[V, X].
     */
    def empty[V: Ordering, X <: EdgeLike[V], P: HasZero]: OrderedVertexMap[V, X, P] = OrderedVertexMapCase(TreeMap.empty[V, Vertex[V, X, P]])
}

trait UnorderedVertexMap[V, +X <: EdgeLike[V], P] extends VertexMap[V, X, P]

/**
 * Case class to represent an unordered VertexMap,
 * that's to say a VertexMap where V is unordered, typically used for a ConcreteDirectedGraph).
 *
 * @param map a HashMap of V -> Vertex[V, X].
 * @tparam V the (key) vertex-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
 */
case class UnorderedVertexMapCase[V, X <: EdgeLike[V], P: HasZero](map: HashMap[V, Vertex[V, X, P]]) extends BaseVertexMap[V, X, P](map) with UnorderedVertexMap[V, X, P] {

    /**
     * Method to construct a new UnorderedVertexMapCase from the given map.
     *
     * @param map a HashMap. If it is not a HashMap, it will be converted to one.
     * @return a new UnorderedVertexMapCase[V, X].
     */
    def unit[Y >: X <: EdgeLike[V]](map: Map[V, Vertex[V, Y, P]]): VertexMap[V, Y, P] = UnorderedVertexMapCase[V, Y, P](map.to(HashMap))
}

/**
 * Companion object to UnorderedVertexMapCase.
 */
object UnorderedVertexMap {
    def apply[V, X <: EdgeLike[V], P: HasZero](v: V): VertexMap[V, X, P] = empty[V, X, P].addVertex(v)

    /**
     * Method to yield an empty UnorderedVertexMapCase.
     *
     * @tparam V the (key) vertex-attribute type.
     * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
     * @return an empty UnorderedVertexMapCase[V, X].
     */
    def empty[V, X <: EdgeLike[V], P: HasZero]: UnorderedVertexMap[V, X, P] = UnorderedVertexMapCase(HashMap.empty[V, Vertex[V, X, P]])
}

/**
 * Abstract base class to define general VertexMap properties.
 *
 * CONSIDER rename Base as Abstract
 *
 * @param _map a Map of V -> Vertex[V, X].
 * @tparam V the (key) vertex-attribute type.
 * @tparam X the type of edge which connects two vertices. A sub-type of EdgeLike[V].
 */
abstract class BaseVertexMap[V, +X <: EdgeLike[V], P: HasZero](val _map: Map[V, Vertex[V, X, P]]) extends VertexMap[V, X, P] {

    require(_map != null, "BaseVertexMap: _map is null")

    def contains(v: V): Boolean = _map.contains(v)

    def size: Int = _map.size

    /**
     * Method to get the AdjacencyList for vertex with key (attribute) v, if there is one.
     *
     * @param v the key (attribute) of the vertex whose adjacency list we require.
     * @return an Option of AdjacencyList[X].
     */
    def optAdjacencyList(v: V): Option[AdjacencyList[X]] = _map.get(v) map (_.adjacent)

    /**
     * Method to add a vertex of (key) type V to this graph.
     * The vertex will have degree of zero.
     *
     * @param v the (key) attribute of the result.
     * @return a new AbstractGraph[V, E, X].
     */
    def addVertex(v: V): VertexMap[V, X, P] = unit(_map + (v -> (_map.get(v) match {
        case Some(vv) => vv
        case None => Vertex.empty(v)
    })))

    /**
     * Method to add an edge to this VertexMap.
     *
     * @param v the (key) value of the vertex whose adjacency list we are adding to.
     * @param y the edge to be added to the adjacency list.
     * @tparam Y a super-type of X.
     * @return a new VertexMap which includes all the original entries of <code>this</code> plus <code>v -> x</code>.
     */
    def addEdge[Y >: X <: EdgeLike[V]](v: V, y: Y): VertexMap[V, Y, P] = unit(
        _map.get(v) match {
            case Some(vv) => buildMap(_map - v, v, y, vv)
            case None => buildMap(_map, v, y, Vertex.empty(v))
        }
    )

    /**
     * The map of V -> Vertex[V, X] elements.
     */
    val vertexMap: Map[V, Vertex[V, X, P]] = _map

    /**
     * the vertex-type values, i.e. the keys, of this VertexMap.
     */
    val keys: Iterable[V] = _map.keys

    /**
     * the Vertex[V, X] values of this VertexMap.
     */
    def values[Y >: X <: EdgeLike[V]]: Iterable[Vertex[V, Y, P]] = _map.values

    /**
     * the X values of this VertexMap.
     */
    val edges: Iterable[X] = _map.values.flatMap(_.adjacent.xs)

    /**
     * Method to run depth-first-search on this VertexMap.
     *
     * @param visitor the visitor, of type Visitor[V, J].
     * @param v       the starting vertex.
     * @tparam J the journal type.
     * @return a new Visitor[V, J].
     */
    def dfs[J](visitor: Visitor[V, J])(v: V): Visitor[V, J] = {
        initializeVisits(v)
        val result = recursiveDFS(visitor, v)
        result.close()
        result
    }

    /**
     * Method to run breadth-first-search on this VertexMap.
     *
     * @param visitor the visitor, of type Visitor[V, J].
     * @param v       the starting vertex.
     * @tparam J the journal type.
     * @return a new Visitor[V, J].
     */
    def bfs[J](visitor: Visitor[V, J])(v: V): Visitor[V, J] = {
        initializeVisits(v)
        implicit object queuable extends QueueableQueue[V]
        val result: Visitor[V, J] = doBFSImmutable[J, Queue[V]](visitor, v)
        result.close()
        result
    }

    /**
     * Method to run breadth-first-search with a mutable queue on this Traversable.
     *
     * @param visitor the visitor, of type Visitor[V, J].
     * @param v       the starting vertex.
     * @tparam J the journal type.
     * @tparam Q the type of the mutable queue for navigating this Traversable.
     *           Requires implicit evidence of MutableQueueable[Q, V].
     * @return a new Visitor[V, J].
     */
    def bfsMutable[J, Q](visitor: Visitor[V, J])(v: V)(implicit ev: MutableQueueable[Q, V]): Visitor[V, J] = {
        initializeVisits(v)
        val result: Visitor[V, J] = doBFSMutable[J, Q](visitor, v)
        result.close()
        result
    }

    /**
     * (abstract) Method to construct a new VertexMap from the given map.
     *
     * @param map a Map (might be TreeMap or HashMap).
     * @return a new VertexMap[V, X].
     */
    def unit[Y >: X <: EdgeLike[V]](map: Map[V, Vertex[V, Y, P]]): VertexMap[V, Y, P]

    /**
     * Non-tail-recursive method to run DFS on the vertex V with the given Visitor.
     *
     * @param visitor the Visitor[V, J].
     * @param v       the vertex at which we run depth-first-search.
     * @tparam J the Journal type of the Visitor.
     * @return a new Visitor[V, J].
     */
    private def recursiveDFS[J](visitor: Visitor[V, J], v: V): Visitor[V, J] =
        recurseOnVertex(v, visitor.visitPre(v)).visitPost(v)

    private def recurseOnVertex[J](v: V, visitor: Visitor[V, J]) = optAdjacencyList(v) match {
        case Some(xa) => xa.xs.foldLeft(visitor)((q, x) => recurseOnEdgeX(v, q, x))
        case None => throw GraphException(s"DFS logic error 0: recursiveDFS(v = $v)")
    }

    private def recurseOnEdgeX[J, Y >: X <: EdgeLike[V]](v: V, visitor: Visitor[V, J], y: Y) =
        VertexMap.findAndMarkVertex(vertexMap, y.other(v), s"DFS logic error 1: findAndMarkVertex(v = $v, x = $y") match {
            case Some(z) => recursiveDFS(visitor, z)
            case None => visitor
        }

    private def enqueueUnvisitedVertices[Q](v: V, queue: Q)(implicit queueable: Queueable[Q, V]): Q = optAdjacencyList(v) match {
        case Some(xa) => xa.xs.foldLeft(queue)((q, x) => queueable.appendAll(q, getVertices(v, x)))
        case None => throw GraphException(s"BFS logic error 0: enqueueUnvisitedVertices(v = $v)")
    }

    private def getVertices[Y >: X <: EdgeLike[V]](v: V, y: Y): Seq[V] = findAndMarkVertex(vertexMap, y.other(v), "getVertices").toSeq

    private def doBFSImmutableX[J, Q](visitor: Visitor[V, J], queue: Q)(implicit queueable: Queueable[Q, V]): Visitor[V, J] = {
        @tailrec
        def inner(result: Visitor[V, J], work: Q): Visitor[V, J] = queueable.take(work) match {
            case Some((head, tail)) => inner(result.visitPre(head), enqueueUnvisitedVertices(head, tail))
            case _ => result
        }

        inner(visitor, queue)
    }

    private def doBFSImmutable[J, Q](visitor: Visitor[V, J], v: V)(implicit queueable: Queueable[Q, V]): Visitor[V, J] =
    // CONSIDER inlining this method
        doBFSImmutableX(visitor, queueable.append(queueable.empty, v))

    private def doBFSMutableX[J, Q](visitor: Visitor[V, J], queue: Q)(implicit queueable: MutableQueueable[Q, V]): Visitor[V, J] = {
        @tailrec
        def inner(result: Visitor[V, J], work: Q): Visitor[V, J] = {
            queueable.take(work) match {
                case Some(v) => inner(result.visitPre(v), enqueueMutableUnvisitedVertices(v, work))
                case _ => result
            }
        }

        inner(visitor, queue)
    }

    private def doBFSMutable[J, Q](visitor: Visitor[V, J], v: V)(implicit queueable: MutableQueueable[Q, V]): Visitor[V, J] = {
        val queue: Q = queueable.empty
        queueable.append(queue, v)
        // CONSIDER inlining this method
        doBFSMutableX(visitor, queue)
    }

    private def enqueueMutableUnvisitedVertices[Q](v: V, queue: Q)(implicit queueable: MutableQueueable[Q, V]): Q = optAdjacencyList(v) match {
        case Some(xa) => xa.xs.foldLeft(queue) { (q, x) => queueable.appendAll(q, getVertices(v, x)); queue }
        case None => throw GraphException(s"BFS logic error 0: enqueueUnvisitedVertices(v = $v)")
    }

    private def initializeVisits[J](v: V): Unit = {
        vertexMap.values foreach (_.reset())
        VertexMap.findAndMarkVertex(vertexMap, Some(v), s"initializeVisits")
    }

    /**
     * Build a VertexMap from the given map (m) and the edge y at vertex v.
     * TODO revert to private.
     *
     * @param m  the existing Map.
     * @param v  the vertex (key) at which to update the adjacency list.
     * @param y  the edge to be added.
     * @param vv the existing adjacency list for vertex v.
     * @tparam Y the type of the ege to be added.
     * @return a new Map.
     */
    def buildMap[Y >: X <: EdgeLike[V]](m: Map[V, Vertex[V, Y, P]], v: V, y: Y, vv: Vertex[V, Y, P]): Map[V, Vertex[V, Y, P]] = m + (v -> (vv addEdge y))
}

object VertexMap {
    /**
     * This method finds the vertex at the other end of x from v, checks to see if it is already discovered
     * and, if not, marks it as discovered then returns it, wrapped in Some.
     *
     * @return Option[V]: the (optional) vertex to run dfs on next.
     */
    private[core] def findAndMarkVertex[V, X <: EdgeLike[V], P: HasZero](vertexMap: Map[V, Vertex[V, X, P]], maybeV: Option[V], errorMessage: String): Option[V] = maybeV match {
        case Some(z) =>
            val vXvo: Option[Vertex[V, X, P]] = vertexMap.get(z)
            val qo: Option[V] = vXvo filterNot (_.discovered) map (_.attribute)
            qo match {
                case Some(q) =>
                    Some(q) // CONSIDER check that q eq z
                case None =>
                    None
            }
        case None => throw GraphException(errorMessage)
    }
}
