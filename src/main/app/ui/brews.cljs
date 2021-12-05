(ns app.ui.brews
  (:require
   [app.model.session :as session]
   [app.model.brew :as brew]
   [app.ui.design-system :as ds]
   [app.application :refer [SPA]]
   [clojure.string :as str]
   [com.fulcrologic.fulcro.dom :as dom :refer [div ul li p h3 button b]]
   [com.fulcrologic.fulcro.dom.html-entities :as ent]
   [com.fulcrologic.fulcro.dom.events :as evt]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
   [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]
   [com.fulcrologic.fulcro-css.css :as css]
   [com.fulcrologic.fulcro.algorithms.form-state :as fs]
   [taoensso.timbre :as log]
   [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]))

(defsc BeanSelectOption [this {:bean/keys [id name]}]
  {:query [:bean/id :bean/name]
   :ident :bean/id}
  (dom/option {:value id} name))

(def ui-bean-select-option (comp/factory BeanSelectOption))

#_(defsc BeanSelect [this {:select/keys [id options] :ui/keys [value]}]
    {:query [:select/id
             {:select/options (comp/get-query BeanSelectOption)}
             :ui/value]
     :ident :select/id
     #_#_:initial-state {:select/id :params/id :select/options []}
     :componentDidMount (fn [this] (df/load! this :all-beans BeanSelectOption {:target [:select/id :all-beans :select/options]}))}
    (dom/select {:value (or value "")}
                (map ui-bean-select-option options)))

#_(def ui-bean-select (comp/factory BeanSelect))

(defsc BrewForm [this {:brew/keys [id dose yield duration bean] :bean/keys [options]}]
  {:query [:brew/id
           :brew/dose
           :brew/yield
           :brew/duration
           :brew/bean
           {[:bean/options '_] (comp/get-query BeanSelectOption)}
           fs/form-config-join]
   :ident :brew/id
   :form-fields #{:brew/dose :brew/yield :brew/duration}
   :componentDidMount (fn [this] (df/load! this :all-beans BeanSelectOption {:target [:bean/options]}))}
  (dom/div
   "brew form"
   (dom/form {:onSubmit (fn [e]
                          (.preventDefault e)
                          (tap> "submitting"))}

             (dom/select {:value (or bean "")
                          :onChange #(m/set-value! this :brew/bean (uuid (.. % -target -value)))}
                         (dom/option {:value "" :disabled true} "--")
                         (map ui-bean-select-option options))

             (ds/ui-label {:htmlFor "brew-form-dose"} "Dose")
             (ds/ui-float-input
              this
              {:id "brew-form-yield"
               :value dose
               :name :brew/dose})

             (ds/ui-label {:htmlFor "brew-form-yield"} "Yield")
             (ds/ui-float-input
              this
              {:id "brew-form-yield"
               :value yield
               :name :brew/yield})

             (ds/ui-label {:htmlFor "brew-form-duration"} "Duration")
             (ds/ui-float-input
              this
              {:id "brew-form-duration"
               :value duration
               :name :brew/duration}))))

(def ui-brew-form (comp/factory BrewForm))

(defsc BrewPage [this {:keys [brew-form]}]
  {:query [{:brew-form (comp/get-query BrewForm)}]
   :ident (fn [] [:component/id :brew-page])
   :route-segment ["brews"]
   :initial-state {:brew-form {}}}
  (dom/div
   "Brew page"
   (if brew-form
     (ui-brew-form brew-form)
     (ds/ui-button {:onClick #(comp/transact! this `[(brew/edit-new-brew)])} "New brew"))))
