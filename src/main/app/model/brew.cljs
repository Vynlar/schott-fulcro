(ns app.model.brew
  (:require
   [clojure.spec.alpha :as s]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]
   [com.fulcrologic.fulcro.algorithms.tempid :refer [tempid]]
   [com.fulcrologic.fulcro.algorithms.form-state :as fs]
   [com.fulcrologic.fulcro.components :as comp]))

(s/def :brew/dose (s/and float pos?))

(defmutation edit-new-brew [_]
  (action [{:keys [state app]}]
          (let [id (tempid)
                brew-entity {:brew/id id
                             :brew/dose 18
                             :brew/yield nil
                             :brew/duration nil
                             :brew/bean nil
                             :ui/bean-options []
                             #_#_:ui/bean-select {:select/id :all-beans
                                                  :select/options []}}
                brew-form-class (comp/registry-key->class :app.ui.brews/BrewForm)]
            (merge/merge-component! app brew-form-class brew-entity :replace [:component/id :brew-page :brew-form]))))
