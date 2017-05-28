(ns braid.test.client.runners.tests
  "A namespace that acts as a proxy for test runners to require all
  client test namespaces. This is useful if we ever have multiple test
  runners (e.g. doo, devcards, etc). New test namespaces need to be
  required here to ensure they are loaded, otherwise test runners
  cannot find them."
  (:require
   [braid.test.client.ui.views.message-test]))
