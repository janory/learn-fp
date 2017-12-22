package learnfp.transformer


import learnfp.functor.{Functor, FunctorOps, Writer}
import learnfp.functor.FunctorOps._
import learnfp.monad.{Monad, MonadOps}
import learnfp.monad.MonadOps._
import learnfp.monoid.Monoid
import learnfp.monoid.MonoidOps._
import learnfp.functor.WriterInstance._
import learnfp.monad.WriterInstance._

case class WriterT[A, M[_], W](runWriterT:M[Writer[W, A]])(implicit f:Functor[M], m:Monad[M], w:Monoid[W])

object WriterT {
  implicit def writerTFunctorInstance[W, M[_]](implicit f:Functor[M], m:Monad[M], w:Monoid[W]) =
    new Functor[({type E[X] = WriterT[X, M, W]})#E] {
      override def fmap[A, B](a: WriterT[A, M, W])(fx: A => B): WriterT[B, M, W] = WriterT {
        f.fmap(a.runWriterT){ av:Writer[W, A] =>
          av fmap {avv => fx(avv)}
        }
      }
    }

  implicit def toFunctorOps[A, M[_], W](a:WriterT[A, M, W])(implicit f:Functor[M], m:Monad[M], w:Monoid[W]):FunctorOps[A, ({type E[X] = WriterT[X, M, W]})#E] =
    new FunctorOps[A, ({type E[X] = WriterT[X, M, W]})#E](a)


  implicit def writerTMonadInstance[W, M[_]](implicit f:Functor[M], m:Monad[M], w:Monoid[W]) =
    new Monad[({type E[X] = WriterT[X, M, W]})#E]() {
      override def pure[A](a: A): WriterT[A, M, W] = WriterT { m.pure(writerMonadInstance.pure(a)) }
      override def flatMap[A, B](a: WriterT[A, M, W])(fx: A => WriterT[B, M, W]): WriterT[B, M, W] = WriterT {
          m.flatMap(a.runWriterT) {
          av:Writer[W, A] =>
            val (aw, avv) = av.run()
            f.fmap(fx(avv).runWriterT) { bw =>
              val (bww, bv) = bw.run()
              Writer[W, B] {() => (bw.monoid.mappend(aw, bww), bv)} }
        }
      }
    }

  def lift[A,M[_], W](am:M[A])(implicit f:Functor[M], m:Monad[M], w:Monoid[W]):WriterT[A, M, W] = WriterT {
    f.fmap(am) { a => writerMonadInstance[W].pure(a)}
  }

  implicit def writerTToMonadOps[A, M[_], W](a:WriterT[A, M, W])(implicit f:Functor[M], m:Monad[M], w:Monoid[W]) =
    new MonadOps[A, ({type E[X] = WriterT[X, M, W]})#E](a)


  def pure[A, M[_], W](a:A)(implicit f:Functor[M], m:Monad[M], w:Monoid[W]) = WriterT {
     m.pure(writerMonadInstance.pure(a))
  }

  implicit def writerTMonadTransInstance[A, M[_], W](a:A)(implicit f:Functor[M], m:Monad[M], w:Monoid[W]) =
    new MonadTransformer[M, ({type E[X, Y[_]] = WriterT[X, Y, W]})#E] {
      override def lift[A](a: M[A]): WriterT[A, M, W] = WriterT.lift(a)
    }

  def tell[M[_], W](a:W)(implicit f:Functor[M], m:Monad[M], w:Monoid[W]):WriterT[Unit, M, W] = WriterT {
    m.pure { Writer.tell(a)}
  }
}