;; Copyright (c) 2016 Juan Pedro Bolivar Puente <raskolnikov@gnu.org>
;;
;; This file is part of Mittagessen.
;;
;; Mittagessen is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as
;; published by the Free Software Foundation, either version 3 of the
;; License, or (at your option) any later version.
;;
;; Mittagessen is distributed in the hope that it will be useful, but
;; WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; Affero General Public License for more details.
;;
;; You should have received a copy of the GNU Affero General Public
;; License along with Mittagessen.  If not, see
;; <http://www.gnu.org/licenses/>.

(ns mittagessen.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]]))

(defonce app-state
  (r/atom {:data nil
           :choice nil
           :choosing false}))

(defn game-view [state]
  (r/with-let
    [choose (fn []
              (go
                (swap! state assoc-in [:choosing] true)
                (doseq [i (range 10)]
                  (swap! state assoc-in [:choice]
                         (rand-nth (:data @state)))
                  (<! (timeout 100)))
                (swap! state assoc-in [:choosing] false)))]

    (if-let [choice (:choice @state)]
      ;; We already have a choice
      [:div.centered
       [:h1 (:name choice)]
       (when-not (:choosing @state)
         [:div.absolute
          [:div.button
           {:style {:border-color (:fg choice)}
            :on-click choose} "Again?"]])]
      ;; First time question
      [:div.centered
       [:h1 "Where should we go for lunch?"]
       [:div.absolute [:div.button {:on-click choose} "Choose!"]]])))

(defn root-view [state]
  (go
    (let [data (:body (<! (http/get "data/places.json")))]
      (swap! state assoc-in [:data] data)))
  (fn [state]
    [:div.content
     (when-let [choice (:choice @state)]
       {:style {:background-color (:bg choice)
                :color (:fg choice)}})
     (when (:data @state)
       [game-view state])]))

(defn init-app! []
  (enable-console-print!)
  (prn "Mittagsessen app started!")
  (r/render-component
   [root-view app-state]
   (.getElementById js/document "components")))

(defn on-figwheel-reload! []
  (prn "Figwheel reloaded...")
  (swap! app-state update-in [:__figwheel_counter] inc))

(init-app!)
