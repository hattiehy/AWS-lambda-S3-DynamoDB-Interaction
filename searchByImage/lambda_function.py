import base64
import boto3
import uuid
import logging
from urllib.parse import unquote_plus
import cv2
import numpy as np
import json


from boto3.dynamodb.conditions import Key

logger = logging.getLogger()
logger.setLevel(logging.INFO)
s3_client = boto3.client('s3')

# open yolo3 files
yolo_s3 = 'fit5225yoloandlibs'
weight = s3_client.get_object(Bucket=yolo_s3, Key='yolov3-tiny.weights')
config = s3_client.get_object(Bucket=yolo_s3, Key='yolov3-tiny.cfg')
labels = s3_client.get_object(Bucket=yolo_s3, Key='coco.names')
label_lst = labels['Body'].read().decode('utf8').split('\n')
net = cv2.dnn.readNetFromDarknet(config['Body'].read(), weight['Body'].read())


# defend a method to do predictions
def do_prediction(image, net, label_lst) -> list:
    (H, W) = image.shape[:2]
    # get layers inside the model
    layer_names = net.getLayerNames()
    layer_names = [layer_names[i[0] - 1] for i in net.getUnconnectedOutLayers()]

    # process the image
    blob = cv2.dnn.blobFromImage(image, 1 / 255.0, (416, 416), swapRB=True, crop=False)
    net.setInput(blob)
    layer_outputs = net.forward(layer_names)

    # boxes is used to save coordinates of items been predicted
    boxes = []
    # confidences is used to save possibility
    confidences = []
    # ID is the id of the items which has been predicted
    classIDs = []

    # for loop of all outputs
    for output in layer_outputs:
        # for loop of each layer
        for detection in output:
            # extract the class ID and confidence (i.e., probability) of the current object detection
            scores = detection[5:]
            classID = np.argmax(scores)
            confidence = scores[classID]

            # threshold has been set to 0.5
            if confidence > 0.5:
                box = detection[0:4] * np.array([W, H, W, H])
                (centerX, centerY, width, height) = box.astype("int")

                # calculate the top and left corner of pic
                x = int(centerX - (width / 2))
                y = int(centerY - (height / 2))

                boxes.append([x, y, int(width), int(height)])
                confidences.append(float(confidence))
                classIDs.append(classID)
    #
    idxs = cv2.dnn.NMSBoxes(boxes, confidences, 0.3, 0.1)

    tag_lst = []
    if len(idxs) > 0:
        for i in idxs.flatten():
            tag_lst.append(label_lst[classIDs[i]])

    return tag_lst


def query_object(uid, dynamodb=None):
    if not dynamodb:
        dynamodb = boto3.resource('dynamodb')

    table = dynamodb.Table('TagRecord')

    response = table.query(
        KeyConditionExpression=Key('uid').eq(uid)
    )
    return response['Items']

def process_encode_image(encode_image):
    header, data = encode_image.split(',', 1)
    img = base64.b64decode(data)
    # Convert base64 to opencv's Mat format
    nparr = np.fromstring(img, np.uint8)
    img_np = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    return img_np


def lambda_handler(event, context):
    #logger.info(event)
    request = eval(event['body'])
    logger.info(request)
    uid = request['uid']
    encode_image = request['image']
    #logger.info('UID: ' + uid)
    img_np = process_encode_image(encode_image)
    tags = do_prediction(img_np, net, label_lst)
    logger.info('TAGS: ' + str(tags))
    object_lst = query_object(uid)

    resp_lst = []
    for ob in object_lst:
        if len(set(tags)) == 0:
            response = {
                "statusCode": 200,
                "headers": {},
                "body": json.dumps({
                "objects": []
                })
            }
        elif set(tags) < set(ob['tags']):
            ob['tags'] = list(ob['tags'])
            resp_lst.append(ob)

    response = {
                "statusCode": 200,
                "headers": {},
                "body": json.dumps({
                "objects": resp_lst
                })
            }


    return response