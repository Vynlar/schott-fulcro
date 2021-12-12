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

(def ui-bean-select-option (comp/factory BeanSelectOption {:keyfn :bean/id}))

(defsc BrewForm [this {:brew/keys [id dose yield duration bean] :ui/keys [bean-options] :as props}]
  {:query [:brew/id
           :brew/dose
           :brew/yield
           :brew/duration
           :brew/bean
           {:ui/bean-options (comp/get-query BeanSelectOption)}
           fs/form-config-join]
   :ident :brew/id
   :form-fields #{:brew/id :brew/dose :brew/yield :brew/duration :brew/bean}
   :componentDidMount (fn [this] (df/load! this :all-beans BeanSelectOption
                                           {:target [:brew/id (:brew/id (comp/props this)) :ui/bean-options]}))}
  (dom/div
   "brew form"
   (dom/form {:onSubmit (fn [e]
                          (.preventDefault e)
                          (comp/transact! this [(brew/save-brew {:brew/id id
                                                                 :values (fs/dirty-fields props false)})]))}

             (dom/select {:value (str (or bean ""))
                          :onChange #(m/set-value! this :brew/bean (uuid (.. % -target -value)))}
                         (dom/option {:value "" :disabled true} "--")
                         (map ui-bean-select-option bean-options))

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
               :name :brew/duration})

             (ds/ui-button {:type "submit"} "Save"))))

(def ui-brew-form (comp/factory BrewForm))

(defsc BrewListItem [this {:brew/keys [dose yield duration bean]}]
  {:query [:brew/id :brew/dose :brew/yield :brew/duration {:brew/bean [:bean/id :bean/name]}]
   :ident :brew/id}
  (dom/li "Dose: " dose " Yield: " yield " Duration: " duration
          " Bean: " (dom/a {:onClick (fn [] (dr/change-route this ["beans" (:bean/id bean)]))}
                           (:bean/name bean))))

(def ui-brew-list-item (comp/factory BrewListItem {:keyfn :brew/id}))

(defsc BrewList [this {:list/keys [brews]}]
  {:query [:list/id {:list/brews (comp/get-query BrewListItem)}]
   :ident :list/id
   :initial-state {:list/id :param/id
                   :list/brews []}
   :componentDidMount
   (fn [this]
     (df/load! this :all-brews BrewListItem {:target [:list/id (:list/id (comp/props this)) :list/brews]}))}

  (dom/div
   (dom/h2 "Brews")
   (dom/ul
    (map ui-brew-list-item brews))))

(def ui-brew-list (comp/factory BrewList))

(defsc BrewPage [this {:keys [brew-form brew-list]}]
  {:query [{:brew-form (comp/get-query BrewForm)}
           {:brew-list (comp/get-query BrewList)}]
   :ident (fn [] [:component/id :brew-page])
   :route-segment ["brews"]
   :initial-state {:brew-form {}
                   :brew-list {:id :all-brews}}}
  (dom/div
   "Brew page"
   (if brew-form
     (ui-brew-form brew-form)
     (ds/ui-button {:onClick #(comp/transact! this `[(brew/edit-new-brew)])} "New brew"))
   (ui-brew-list brew-list)))
