
;;;;
;;;; max2-pair.sl - The max2pair example problem encoded in SemGuS
;;;;

;;; Metadata
(set-info :format-version "2.0.0")
(set-info :author("Boying Li"))
(set-info :realizable true)

;;;
;;; Term types
;;;
(declare-term-types
 ;; Nonterminals
 ((A 0) (B 0))

 ;; Productions
 ((($it); E productions
   )

  (($calc ($calc1 A) ($calc2 A)) ; B productions
   )))

;;;
;;; Semantics
;;;
(define-funs-rec
    ;; CHC heads
    ((A.Sem ((at A) (x Int) (y Int) (c Int) (d Int)) Bool)
     (B.Sem ((bt B) (x Int) (y Int) (r Int)) Bool))

  ;; Bodies
  ((! (match at ; A.Sem definitions
       (
        ($it (and (A.Sem at x y c d) (and (= c x) (= d y))))
       ))

    :input (x y) :output (c d))

   (! (match bt ; B.Sem definitions
        (
         (($calc at1 at2)
          (exists ((c1 Int) (d1 Int) (c2 Int) (d2 Int))
              (and
               (A.Sem at1 x y c1 d1)
               (A.Sem at2 x y c2 d2)
               (= r (+ (* c1 d2) (* c2 d1))))))

         ))
    :input (x y) :output (r))))

;;;
;;; Function to synthesize - a term rooted at E
;;;
(synth-fun foo() B) ; Using the default universe of terms rooted at E

;;;
;;; Constraints - examples
;;;
(constraint (B.Sem foo 1 2 4))

;;;
;;; Instruct the solver to find max2
;;;
(check-synth)
