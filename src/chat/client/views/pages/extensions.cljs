(ns chat.client.views.pages.extensions
  (:require [om.core :as om]
            [om.dom :as dom]
            [chat.client.store :as store]))

(defn new-asana-form
  [data owner]
  (dom/form #js {}
    (dom/input #js {:type "text" :placeholder "foobar"})))

; TODO: how to get list of available extensions?
(def available-extensions
  {"asana" new-asana-form})

(defn new-extension-view
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:type nil})
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "new-extension"}
        (dom/pre nil (pr-str state))
        (apply dom/select
          #js {:className "extension-select"
               :onChange (fn [e]
                           (om/set-state! owner :type (.. e -target -value)))}
          (map (fn [t] (dom/option #js {:value t} ((fnil name "") t)))
               (cons nil (keys available-extensions))))
        ((get available-extensions (state :type) (constantly nil)) data owner)))))

(defn extensions-page-view
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "page extensions"}
        (dom/h1 nil "Extensions")
        (apply dom/div #js {:className "extensions-list"}
          (map (fn [ext]
                 (dom/div #js {:className "extension"}
                   (str (ext :id) ": " (name (ext :type)))))
               (get-in data [:groups (data :open-group-id) :extensions])))
        (om/build new-extension-view data)))))
