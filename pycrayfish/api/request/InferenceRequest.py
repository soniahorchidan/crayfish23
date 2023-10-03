import requests


class InferenceRequest:
    def makeRequest(self, request: str, url: str):
        response = requests.get(url=url, params=request)
        if response.status_code == 200:  # success
            predictionStr = str(response).replace("[", "").replace("]", "")
            return predictionStr
        else:
            print('POST request failed! ' + str(response.status_code))

        return None
