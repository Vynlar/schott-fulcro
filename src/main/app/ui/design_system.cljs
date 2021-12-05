(ns app.ui.design-system
  (:require
   [com.fulcrologic.fulcro.mutations :as m]
   [com.fulcrologic.fulcro.dom :as dom :refer [div ul li p h3 button b]]))

(defn ui-button [params children]
  (dom/button :.bg-blue-500.h-10.px-6.rounded.text-white.font-bold params children))

(defn ui-input [params]
  (dom/input :#bean-form-name.h-10.border.rounded.px-3 params))

(defn handle-float-event [this key]
  (fn [e]
    (let [raw-val (.. e -target -value)
          converted-val (cond
                          (= raw-val "") nil
                          (js/isNaN (js/parseFloat raw-val)) nil
                          :else (js/parseFloat raw-val))]
      (m/set-value! this key converted-val))))

(defn ui-float-input [this {:keys [value name] :as props}]
  (ui-input
   (merge props
          {:type "decimal"
           :value (if (nil? value) "" value)
           :onChange (handle-float-event this name)})))

(defn ui-label [params children]
  (dom/label params children))
