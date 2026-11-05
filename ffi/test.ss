(import (chezscheme))

(load-shared-object "target/libsample.dylib")
(define create-isolate
  (foreign-procedure __collect_safe "graal_create_isolate" (void* void* void*) int))
(define tear-down
  (foreign-procedure __collect_safe "graal_tear_down_isolate" (void*) int))
(define add
  (foreign-procedure __collect_safe "sample_add" (void* integer-64 integer-64) integer-64))

(let ([isolate-cell (foreign-alloc (foreign-sizeof 'void*))]
      [thread-cell (foreign-alloc (foreign-sizeof 'void*))])
  (dynamic-wind
    void
    (lambda ()
      (let ([status (create-isolate 0 isolate-cell thread-cell)])
        (unless (zero? status) (error 'create-isolate "Graal initialization failed" status)))
      (let ([thread (foreign-ref 'void* thread-cell 0)])
        (dynamic-wind
          void
          (lambda ()
            (for-each
              (lambda (case)
                (let ([result (add thread (car case) (cadr case))])
                  (unless (= result (caddr case)) (error 'sample-add "Unexpected result" case result))
                  (printf "~s + ~s = ~s\n" (car case) (cadr case) result)))
              '((2 3 5) (-7 2 -5) (5000000000 7 5000000007))))
          (lambda ()
            (let ([status (tear-down thread)])
              (unless (zero? status) (error 'tear-down "Graal shutdown failed" status)))))))
    (lambda ()
      (foreign-free thread-cell)
      (foreign-free isolate-cell))))
