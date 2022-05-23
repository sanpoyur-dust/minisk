
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
 ((E 0) (B 0))

 ;; Productions
 ((($x); E productions
   ($0)
   ($1)
   ($+ ($+_1 E) ($+_2 E))
   ($ite($ite_1 B) ($ite_2 E) ($ite_3 E)))

  (($t) ; B productions
   ($f)
   ($not ($not_1 B))
   ($and($and_1 B) ($and_2 B))
   ($or($or_1 B) ($or_2 B))
   ($< ($<_1 E) ($<_2 E)))))

;;;
;;; Semantics
;;;
(define-funs-rec
    ;; CHC heads
    ((E.Sem ((et E) (xa Int) (xb Int) (ya Int) (yb Int) (ra Int) (rb Int)) Bool)
     (B.Sem ((bt B) (xa Int) (xb Int) (ya Int) (yb Int) (r Bool)) Bool))

  ;; Bodies
  ((! (match et ; E.Sem definitions
       (($x (and (E.Sem et xa xb ya yb ra rb) (and (= ra xa) (= rb xb))))
        ($0 (and (E.Sem et xa xb ya yb ra rb) (and (= ra 0) (= rb 0))))
        ($1 (and (E.Sem et xa xb ya yb ra rb) (and (= ra 1) (= rb 1))))
        (($+ et1 et2)
         (exists ((ra1 Int) (rb1 Int) (ra2 Int) (rb2 Int))
             (and
              (E.Sem et1 xa xb ya yb ra1 rb1)
              (E.Sem et2 xa xb ya yb ra2 rb2)
              (and
              (= ra (+ ra1 ra2))
              (= rb (+ rb1 rb2))))))
        (($ite bt1 etc eta)
         (exists ((r0 Bool) (ra1 Int) (rb1 Int) (ra2 Int) (rb2 Int))
             (and
              (B.Sem bt1 xa xb ya yb r0)
              (E.Sem etc xa xb ya yb ra1 rb1)
              (E.Sem eta xa xb ya yb ra2 rb2)
              (and
              (= ra (ite r0 ra1 ra2))
              (= rb (ite r0 rb1 rb2))))))))

    :input (xa xb ya yb) :output (ra rb))

   (! (match bt ; B.Sem definitions
        (($t (= r true))
         ($f (= r false))
         (($not bt1)
          (exists ((rb Bool))
              (and
               (B.Sem bt1 xa xb ya yb rb)
               (= r (not rb)))))
         (($and bt1 bt2)
          (exists ((rb1 Bool) (rb2 Bool))
              (and
               (B.Sem bt1 xa xb ya yb rb1)
               (B.Sem bt2 xa xb ya yb rb2)
               (= r (and rb1 rb2)))))
         (($or bt1 bt2)
          (exists ((rb1 Bool) (rb2 Bool))
              (and
               (B.Sem bt1 xa xb ya yb rb1)
               (B.Sem bt2 xa xb ya yb rb2)
               (= r (or rb1 rb2)))))
         (($< et1 et2)
          (exists ((ra1 Int) (ra2 Int) (rb1 Int) (rb2 Int))
              (and
               (E.Sem et1 xa xb ya yb  ra1 rb1)
               (E.Sem et2 xa xb ya yb  ra2 rb2)
               (= r (< (+ ra1 rb1) (+ ra2 rb2))))))))
    :input (xa xb ya yb) :output (r))))

;;;
;;; Function to synthesize - a term rooted at E
;;;
(synth-fun max2() E) ; Using the default universe of terms rooted at E

;;;
;;; Constraints - examples
;;;
(constraint (E.Sem max2 4 2 3 2 4 2))
(constraint (E.Sem max2 2 5 6 0 2 5))
(constraint (E.Sem max2 2 (+ 3 4) 1 1 2 7))

;;;
;;; Instruct the solver to find max2
;;;
(check-synth)
