(ns app.model.bean-specs
  (:require
   [app.util :refer [uuid]]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen]))

(s/def :bag/id uuid?)
(s/def :bag/roasted-on inst?)
(s/def :bag/bag (s/keys :req [:bag/id :bag/roasted-on]))

(s/def :bean/id uuid?)
(s/def :bean/name (s/and string? #(pos-int? (count %))))
(s/def :bean/bags (s/+ :bag/bag))
(s/def :bean/bean (s/keys :req [:bean/id :bean/name :bean/bags]))

(gen/generate (s/gen :bean/bean))

(comment
  (s/valid? :bean/name "w"))
