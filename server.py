# Note: All keys/passwords have been stripped


from flask import request, Flask
import json
import os, uuid
import httplib, urllib, base64, json
import pexpect
from pexpect import pxssh



UPLOAD_FOLDER = '/home/divyansh/codes/ai/whitecane/images/'
app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

def getCaptions(imagepath):
    ''' Get captions using TensorFlow Show & Tell Model running locally'''
    os.chdir("/home/divyansh/codes/tensorflow/models/im2txt/")
    print "Imagepath = ", imagepath
    output = os.popen("bazel-bin/im2txt/run_inference --checkpoint_path=pretrained/model.ckpt-2000000 --vocab_file=pretrained/word_counts.txt --input_files=" + imagepath).read()
    captions = output.split("\n")[1:-1]
    captions = [caption[5:-13] for caption in captions]
    print "captions = ", captions
    return captions[0]

# def getCaptions(imagepath):
#     ''' Use Microsoft Cognitive Services to generate Captions '''
    
#     print "Imagepath = ", imagepath
    
#     image = open(imagepath, "rb").read()

#     headers = {
#         # Request headers
#         'Content-Type': 'application/octet-stream',
#         'Ocp-Apim-Subscription-Key': '4632ccd8b49b4ec2ad785fad40734f2d',
#     }

#     params = urllib.urlencode({
#         # Request parameters
#         'visualFeatures': 'Description,Faces,Categories',
#         'language': 'en',
#     })

#     conn = httplib.HTTPConnection('10.3.100.207', 8080)
#     conn.request("POST", "http://api.projectoxford.ai/vision/v1.0/analyze?%s" % params, image, headers)
#     response = conn.getresponse()
#     data = response.read()
#     print data
#     data = json.loads(data)
#     print data
#     caption = data['description']['captions'][0]['text']
#     conn.close()
#     return caption

def runYOLO(imagepath):
    os.chdir("/home/divyansh/codes/darknet/")
    print "Imagepath = ", imagepath
    output = os.popen("./darknet detector test cfg/voc.data cfg/tiny-yolo-voc.cfg tiny-yolo-voc.weights -thresh 0.2" + imagepath).read()
    # print output
    tags = output.split("\n")[1:]
    tags = [tag.split(':')[0] for tag in tags]
    print "captions = ", tags
    return tags

def findInImage(imagepath, query):
    print "Imagepath = ", imagepath
    
    image = open(imagepath, "rb").read()

    headers = {
        # Request headers
        'Content-Type': 'application/octet-stream',
        'Ocp-Apim-Subscription-Key': 'xxxxxxx',
    }

    params = urllib.urlencode({
        # Request parameters
        'visualFeatures': 'Description,Faces,Categories',
        'language': 'en',
    })

    conn = httplib.HTTPConnection('10.3.100.207', 8080)
    conn.request("POST", "http://api.projectoxford.ai/vision/v1.0/analyze?%s" % params, image, headers)
    response = conn.getresponse()
    data = response.read()
    print data
    data = json.loads(data)
    print data
    tags = data['description']['tags']
    conn.close()

    if query in tags or query in data['description']['captions'][0]['text']:
        return "Found it"
    else:
        return "Try again"

def answerQuestion(imagepath, question):
    sentinel = open("SENTINEL.txt", "w")
    sentinel.write("images/" + os.path.basename(imagepath) + "\n")
    sentinel.write(question + "\n")
    sentinel.close()

    x = "xxxxx"
    u = "xxxxx"
    child = pexpect.spawn("scp " + imagepath + " xxxx@xxxx:~/whitecane/images/")
    child.expect("password:")
    child.sendline(x)
    child.expect(pexpect.EOF)

    child = pexpect.spawn("scp SENTINEL.txt xxxx@xxxxx:~/whitecane/")
    child.expect("password:")
    child.sendline(x)
    child.expect(pexpect.EOF)

    session = pxssh.pxssh()
    session.login("xxxxxx", u, x)
    # session.sendline('ls whitecane')
    # session.prompt()
    # print session.before
    session.sendline('cd whitecane')
    session.prompt()
    session.sendline('python brahma.py ' + os.path.basename(imagepath))
    session.prompt()

    print "108's brahma.py started"
    print session.before

    answered = False
    while(not answered):
        print "Waiting for 108's reply"
        child = pexpect.spawn("scp xxxx@xxxx:~/whitecane/answer.txt ./")
        child.expect("password:")
        child.sendline(x)
        child.expect(pexpect.EOF)
        print child.before
        if not "No such file" in child.before:
            answered = True
            # session = pxssh.pxssh()
            # session.login(host, u, x)
            # session.sendline('rm ~/whitecane/answer.txt')
            # session.prompt()

    answer = open("answer.txt", 'r')
    return answer.readline()

def getFacesDescription(imagepath):
    ''' Use Microsoft Cognitive Services to generate description of all the faces '''
    return "This feature is not yet available"

    print "Imagepath = ", imagepath
    
    image = open(imagepath, "rb").read()

    headers = {
        # Request headers
        'Content-Type': 'application/octet-stream',
        'Ocp-Apim-Subscription-Key': 'xxxxx',
    }

    params = urllib.urlencode({
        # Request parameters
        'returnFaceId': 'true',
        'returnFaceLandmarks': 'false',
        'returnFaceAttributes': 'true',
    })

    conn = httplib.HTTPConnection('10.3.100.207', 8080)
    conn.request("POST", "https://api.projectoxford.ai/face/v1.0/detect?%s" % params, image, headers)
    response = conn.getresponse()
    data = response.read()
    print data
    data = json.loads(data)
    print data
    caption = data[0]['faceAttributes']['gender']
    
    conn.close()
    return caption


# def classifyIntent(query):
#     ''' Use LUIS to decide which model to use'''
#     return "CAPTION"

@app.route('/upload', methods=['GET', 'POST'])
def upload():
    if request.method == 'POST':
        file = request.files['file']
        extension = os.path.splitext(file.filename)[1]
        f_name = str(uuid.uuid4()) + extension

        filepath = os.path.join(app.config['UPLOAD_FOLDER'], f_name)
        file.save(filepath)

        # query = "lalala"
        query = request.form['text']
        print query
        model = classifyIntent(query)

        if model == 'CAPTION': 
            captions = getCaptions(filepath)
            return json.dumps(captions)
        elif model == 'QA':
            answer = answerQuestion(filepath, query)
            return json.dumps(answer)
        elif model == 'STORY':
            return
    
    if request.method == 'GET':
        return json.dumps({'error' : 1})

@app.route('/qa', methods=['POST'])
def qa():
    if request.method == 'POST':
        file = request.files['file']
        query = request.form['text']
        extension = os.path.splitext(file.filename)[1]
        f_name = str(uuid.uuid4()) + extension

        filepath = os.path.join(app.config['UPLOAD_FOLDER'], f_name)
        file.save(filepath)

        print query        
        answer = answerQuestion(filepath, query)
        return json.dumps(answer)

@app.route('/find', methods=['POST'])
def find():
    if request.method == 'POST':
        file = request.files['file']
        query = request.form['text']
        extension = os.path.splitext(file.filename)[1]
        f_name = str(uuid.uuid4()) + extension

        filepath = os.path.join(app.config['UPLOAD_FOLDER'], f_name)
        file.save(filepath)

        print query        
        answer = findInImage(filepath, query)
        return json.dumps(answer)

@app.route('/caption', methods=['POST'])
def caption():
    if request.method == 'POST':
        file = request.files['file']
        extension = os.path.splitext(file.filename)[1]
        f_name = str(uuid.uuid4()) + extension

        filepath = os.path.join(app.config['UPLOAD_FOLDER'], f_name)
        file.save(filepath)

        captions = getCaptions(filepath)
        return json.dumps(captions)

@app.route('/face', methods=['POST'])
def face():
    if request.method == 'POST':
        file = request.files['file']
        extension = os.path.splitext(file.filename)[1]
        f_name = str(uuid.uuid4()) + extension

        filepath = os.path.join(app.config['UPLOAD_FOLDER'], f_name)
        file.save(filepath)

        captions = getFacesDescription(filepath)
        return json.dumps(captions)


if __name__ == "__main__":
    app.run("0.0.0.0", debug=True)
