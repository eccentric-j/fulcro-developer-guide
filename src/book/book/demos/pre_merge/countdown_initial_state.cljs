(ns book.demos.pre-merge.countdown-initial-state
  (:require
    [fulcro.client :as fc]
    [fulcro.client.data-fetch :as df]
    [book.demos.util :refer [now]]
    [fulcro.client.mutations :as m]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as prim :refer [defsc InitialAppState initial-state]]
    [fulcro.client.data-fetch :as df]
    [fulcro.server :as server]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SERVER:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def all-counters
  [{::counter-id 1 ::counter-label "A"}
   {::counter-id 2 ::counter-label "B" ::counter-initial 10}
   {::counter-id 3 ::counter-label "C" ::counter-initial 2}
   {::counter-id 4 ::counter-label "D"}])

(server/defquery-root ::all-counters
  (value [_ _]
    all-counters))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; CLIENT:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-count 5)

(defsc CountdownButton [this {:ui/keys [count]}]
  {:ident     [:ui/id :ui/id]
   :query     [:ui/id :ui/count]
   :pre-merge (fn [{:keys [current-normalized data-tree]}]
                (merge
                  {:ui/id    (random-uuid)
                   :ui/count default-count}
                  current-normalized
                  data-tree))}
  (let [done? (zero? count)]
    (dom/button {:disabled done?
                 :onClick  #(m/set-value! this :ui/count (dec count))}
      (if done? "Done!" (str count)))))

(def ui-countdown-button (prim/factory CountdownButton {:keyfn ::counter-id}))

(defsc Countdown [this {::keys   [counter-label counter-initial]
                        :ui/keys [counter]}]
  {:ident     [::counter-id ::counter-id]
   :query     [::counter-id ::counter-label ::counter-initial
               {:ui/counter (prim/get-query CountdownButton)}]
   :pre-merge (fn [{:keys [current-normalized data-tree]}]
                (let [initial (prim/nilify-not-found (::counter-initial data-tree))]
                  (merge
                    {:ui/counter (cond-> {} initial (assoc :ui/count initial))}
                    current-normalized
                    data-tree)))}
  (dom/div
    (dom/h4 (str counter-label " [" (or counter-initial default-count) "]"))
    (ui-countdown-button counter)))

(def ui-countdown (prim/factory Countdown {:keyfn ::counter-id}))

(defsc Root [this {::keys [all-counters]}]
  {:initial-state (fn [_] {::all-counters
                           [{::counter-id    (prim/tempid)
                             ::counter-label "X"}
                            {::counter-id    (prim/tempid)
                             ::counter-label "Y"}
                            {::counter-id      (prim/tempid)
                             ::counter-label   "Z"
                             ::counter-initial 9}]})
   :query         [{::all-counters (prim/get-query Countdown)}]}
  (dom/div
    (dom/h3 "Counters")
    (if (seq all-counters)
      (dom/div {:style {:display "flex" :alignItems "center" :justifyContent "space-between"}}
        (mapv ui-countdown all-counters))
      (dom/button {:onClick #(df/load this ::all-counters Countdown)}
        "Load many counters"))))

(defn initialize
  "To be used in :started-callback to pre-load things."
  [app])
