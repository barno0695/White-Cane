import inflect

number = len(list_person)
male = 0
female = 0
smile = []
beard = []
glasses = []
mustache = []

p = inflect.engine()

gender = {
	"male": "he",
	"female": "she"
}


i = 1
for person in list_person:
	person["id"] = i
	i += 1
	if person["face"] > 300:
		person["position"] = "right"
	elif person["face"] < 200:
		person["position"] = "left"
	else:
		person["position"] = "center"

	if person["gender"] == 'male':
		male += 1
	if person["gender"] == 'female':
		female += 1
	if person["smile"] > 0.5:
		smile.append(person)
	if person["facialHair"]["beard"] > 0.7:
		beard.append(person)
	if person["facialHair"]["mustache"] > 0.7:
		mustache.append(person)
	if person["glasses"] != "noGlasses":
		glasses.append(person)


out_str = "" 

if number == 0:
	out_str += "There is no faces in your field of view, Please try to retake the picture ."
elif number == 1:
	out_str += "There is a person in the image ."
else:
	out_str += "There are " + p.number_to_words(number) + " people in the image ."

# Glasses
try:
	if len(glasses) > 1:
		out_str += p.number_to_words(len(glasses)) + " of them are wearing glasses."
	for person in glasses:
		out_str += person["gender"] + " of age " + p.number_to_words(person["age"]) + " is wearing " + person["glasses"]
		if person["smile"] > 0.5:
			out_str += "and " + gender[person["gender"]] + " is " + "smiling."

# Beard
try:
	if len(mustache) > 1:
		out_str += p.number_to_words(len(mustache)) + " of them have mustache"
	for person in mustache:
		out_str = person["gender"] + " of age " + p.number_to_words(person["age"]) + " having a mustache " + "is on the " + person["position"]
		if person["smile"] > 0.5:
			out_str += "and " + gender[person["gender"]] + " is " + "smiling."





