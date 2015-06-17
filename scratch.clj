(import 'com.github.rschmitt.dynamicobject.DynamicObject)
(require '[collection-check :refer :all]
         '[clojure.test.check.generators :as gen])

(def gen-element (gen/tuple gen/int))

(assert-map-like 1e3 (DynamicObject/newInstance DynamicObject) gen-element gen-element)
