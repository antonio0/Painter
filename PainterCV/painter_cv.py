import numpy as np
import cv2
import json
from parse_rest.connection import register
from parse_rest.datatypes import Object
from flask import Flask
from flask import request

app = Flask(__name__)
register('9Zxt0AeUGtmpCv7DBX01Wwh3RoT9U6FQbFqkhlnn', 'cIKrUyu4WiAmpfy5LRzYSaPBOWT7PD9vK0MecMiz', master_key='6uWViLrlu1jLzr207XGANB1QRJxz9sfpyIT5eLIr')

class ProcessedImageData(Object):
    pass

def getContoursFromColorImg(color_img):
    ret,thresh = cv2.threshold(color_img,120,255,1)
    thresh = cv2.bitwise_not(thresh)
    contours,h = cv2.findContours(thresh,0,2)
    return contours

def isInView(element, view):
    return element['x'] > view['x'] and element['y'] > view['y'] and (element['width']+element['x']) < (view['width']+view['x']) and (element['y']+element['height']) < (view['y']+view['height'])

def getScaledElement(element, view):
    view_x = view['x']
    view_y = view['y']
    view_width = view['width']
    view_height = view['height']
    element_x = element['x']
    element_y = element['y']
    element_width = element['width']
    element_height = element['height']
    relative_x = element_x - view_x
    relative_y = element_y - view_y
    scaled_x = (relative_x/float(view_width))*1000
    scaled_y = (relative_y/float(view_height))*1000
    scaled_width = (element_width/float(view_width))*1000
    scaled_height = (element_height/float(view_height))*1000
    return {'type':element['type'], 'shape':element['shape'], 'x':int(scaled_x), 'y':int(scaled_y), 'width':int(scaled_width), 'height':int(scaled_height), 'transition_id':0}

def loopContours(contours_list, type, app_data, img):
    for cnt in contours_list:
        if cv2.contourArea(cnt)<5000:
            continue
        min_rect = cv2.boundingRect(cnt)
        approx = cv2.approxPolyDP(cnt,0.03*cv2.arcLength(cnt,True),True)
        if len(approx)==1:
            pass
        elif len(approx)<6:
            print "rectangle"
            this_element = {'type':type, 'shape':'rectangle', 'x':min_rect[0], 'y':min_rect[1], 'width':min_rect[2], 'height':min_rect[3], 'transition_id':0}
            for view in app_data:
                if isInView(this_element, view):
                    scaled_element = getScaledElement(this_element, view)
                    scaled_element['self'] = this_element
                    view['children'].append(scaled_element)
            cv2.drawContours(img,[cnt],0,(0,0,255),5)
        else:
            print "circle"
            this_element = {'type':type, 'shape':'circle', 'x':min_rect[0], 'y':min_rect[1], 'width':min_rect[2], 'height':min_rect[3], 'transition_id':0}
            for view in app_data:
                if isInView(this_element, view):
                    scaled_element = getScaledElement(this_element, view)
                    scaled_element['self'] = this_element
                    view['children'].append(scaled_element)
            cv2.drawContours(img,[cnt],0,(0,255,255),5)

@app.route("/analyze", methods=['GET', 'POST'])
def analyze(disp=False):
    if request.method == 'POST':
        # img_file = 'draw8.jpg'
        img_file = request.files['photo']

        if img_file:
            filename = img_file.filename
            filepath = '/Users/samginsburg/Desktop/Painter/PainterCV/uploads/%s' % (filename)
            img_file.save(filepath)
        else:
            print 'NO IMAGE FILE. USING DEFAULT.'
            img_file = 'draw8.jpg'

        img = cv2.imread(filepath)
        gray = cv2.imread(filepath,0)

        array_alpha = np.array([1.25])
        array_beta = np.array([-30.0])

        # add a beta value to every pixel 
        cv2.add(img, array_beta, img)

        img_hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)

        black_low = (0, 0, 0)
        black_high = (255, 255, 120)
        red_low = (0, 80, 65)
        red_high = (30, 255, 255)
        red_low2 = (155, 80, 65)
        red_high2 = (180, 255, 255)
        green_low = (25, 90, 70)
        green_high = (100, 255, 255)
        blue_low = (100, 80, 65)
        blue_high = (155, 255, 255)

        colorB_low = (65, 0, 0)
        colorB_high = (255, 255, 255)
        colorG_low = (0, 65, 0)
        colorG_high = (255, 255, 255)

        black_img = cv2.inRange(img_hsv, black_low, black_high)

        blue_img = cv2.inRange(img_hsv, blue_low, blue_high)
        # cv2.imshow('img', blue_img)
        blue_img2 = cv2.inRange(img, colorB_low, colorB_high)
        blue_img = cv2.bitwise_and(blue_img, blue_img2)

        red_img = cv2.inRange(img_hsv, red_low, red_high)
        red_img2 = cv2.inRange(img_hsv, red_low2, red_high2)
        red_img = cv2.bitwise_or(red_img, red_img2)
        # cv2.imshow('img', red_img)

        green_img = cv2.inRange(img_hsv, green_low, green_high)
        # cv2.imshow('img', green_img)
        green_img2 = cv2.inRange(img, colorG_low, colorG_high)
        green_img = cv2.bitwise_and(green_img, green_img2)

        black_contours = getContoursFromColorImg(black_img)
        blue_contours = getContoursFromColorImg(blue_img)
        red_contours = getContoursFromColorImg(red_img)
        green_contours = getContoursFromColorImg(green_img)

        app_data = []

        for cnt in black_contours:
            if cv2.contourArea(cnt)<800:
                continue
            min_rect = cv2.boundingRect(cnt)
            approx = cv2.approxPolyDP(cnt,0.03*cv2.arcLength(cnt,True),True)
            if len(approx)==4 or len(approx)==5:
                print "view rectangle"
                app_data.append({'type':'view', 'children':[], 'shape':'rectangle', 'x':min_rect[0], 'y':min_rect[1], 'width':min_rect[2], 'height':min_rect[3], 'id':0})
                cv2.drawContours(img,[cnt],0,(255,0,0),5)
            elif len(approx)>5 and len(approx)<12:
                print "view circle"
                app_data.append({'type':'view', 'children':[], 'shape':'circle', 'x':min_rect[0], 'y':min_rect[1], 'width':min_rect[2], 'height':min_rect[3], 'id':0})
                cv2.drawContours(img,[cnt],0,(0,255,255),5)
            else:
                print 'Unknown shape.'

        # loopContours(black_contours, 'view', app_data)
        loopContours(blue_contours, 'button', app_data, img)
        # loopContours(red_contours, 'transition', app_data, img)
        loopContours(green_contours, 'text_label', app_data, img)

        for cnt in red_contours:
            if cv2.contourArea(cnt)<200:
                continue
            cv2.drawContours(img,[cnt],0,(255,0,0),5)
            min_rect = cv2.boundingRect(cnt)
            cv2.drawContours(img,[cnt],0,(0,255,0),5)

            print "transition"
            this_element = {'type':'transition', 'shape':'rectangle', 'x':min_rect[0], 'y':min_rect[1], 'width':min_rect[2], 'height':min_rect[3]}
            for view in app_data:
                found = False
                for child in view['children']:
                    if child['type']=='button' and isInView(this_element, child['self']):
                        found = True
                        child['transition_id'] = child['transition_id'] + 1
                if not found and isInView(this_element, view):
                    view['id'] = view['id'] + 1

        # print app_data
        app_data_json = json.dumps(app_data)
        app_file = ProcessedImageData(SessionID='demo', ProcessedImageJson=app_data_json)
        app_file.save()
        print app_data_json

        if disp:
            cv2.imshow('img',img)
            cv2.waitKey(0)
            cv2.destroyAllWindows()
        else:
            cv2.imwrite('ResultImage.jpg', img)
        return {'Status':'Success'}
    else:
        return 'Please upload an app photo.'

@app.route('/')
def index():
    return 'Index'

@app.route('/hello')
def hello():
    return 'Hello, World!'

def main():
    analyze()

if __name__ == "__main__":
    app.run(port=8000)
