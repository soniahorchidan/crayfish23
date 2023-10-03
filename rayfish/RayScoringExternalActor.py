import json

import ray
import requests


@ray.remote
class ExternalScoringActor(object):
    def __init__(self, next_actor):
        self.next_actor = next_actor

    def forward(self, msg):
        # print("Scoring actor got", msg)
        response = requests.get("http://10.132.0.16:8000/model", json=msg)
        data = response.text
        self.next_actor.forward.remote(data)
