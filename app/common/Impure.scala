package common

/**
  * Marks a function as being fully impure (has side effects & is non deterministic)
  */
class Impure extends scala.annotation.StaticAnnotation

object Impure {
  /**
    * Marks a function as being non deterministic (partially impure)
    */
  class Nondeterministic extends scala.annotation.StaticAnnotation
  /**
    * Marks a function as having side effects (partially impure)
    */
  class SideEffects extends scala.annotation.StaticAnnotation
}
