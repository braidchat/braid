

        get-state (fn [k] (@state k))

        set-state! (fn [k v] (swap! state assoc k v))

        update-state! (fn
                        ([f] (swap! state f))
                        ([korks f]
                         (if (keyword? korks)
                           (swap! state update-in [korks] f)
                           (swap! state update-in korks f))))

        focus-textbox! (fn [] ) ; TODO



       :component-did-mount
       (fn []
         (when (= (config :thread-id) (store/get-new-thread))
           (store/clear-new-thread!)
           (focus-textbox!)))



       :component-will-update
       (fn []
         (let [next-text (next-state :text)
               ;TODO: get previous text using reagent not om
               prev-text (om/get-render-state owner :text)]
           (when (not= next-text prev-text)
             (put! (next-state :autocomplete-chan) next-text))))
