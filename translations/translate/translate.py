def keywordCount(lines, keyword):
	keywordCount = 0
  
	for i in range(1,lines.__len__()):
		line = lines[i]

		# split words separated by ','
		components = line.split(',')
  
		if components[0] == keyword:
			keywordCount += 1
 
	return keywordCount

def translate(appName, language, index):    
	inputFileName = "../translations.csv"
 
	if (language == "en"):
		outputFileName = "../../app/src/main/res/values/strings.xml"
	else:
		outputFileName = "../../app/src/main/res/values-" + language + "/strings.xml"
 
	outputFile = open( outputFileName, 'w' )
	outputFile.write('<?xml version="1.0" encoding="utf-8"?>\n')
	outputFile.write('<resources xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\n')
	outputFile.write( '    <string name="app_name">' + appName + '</string>\n' )

	inputFile = open(inputFileName, 'r')
	lines = inputFile.readlines()
  
	# step 1: pull out all of the <string> elements
	for i in range(1,lines.__len__()):
		line = lines[i]
  
		# split words separated by ','
		components = line.split(',')

		keyword = components[0]
    
		if keywordCount( lines, keyword ) == 1: # single element
			outputFile.write( '    <string name="' + keyword + '">"' + components[index].strip() + '"</string>\n' )

	currentKeyWord = ''
	needsClosure = False

	# step 2: pull out all of the <string-array> elements
	for i in range(1,lines.__len__()):
		line = lines[i]

		# split words separated by ','
		components = line.split(',')

		keyword = components[0]
    
		if keywordCount( lines, keyword ) > 1: # array of elements
			if keyword != currentKeyWord:
				if needsClosure:
					outputFile.write( '    </string-array>\n' )					
				currentKeyWord = keyword
				outputFile.write( '    <string-array name="' + keyword + '">\n' )
				outputFile.write( '        <item>"' + components[index].strip() + '"</item>\n' )
				needsClosure = True
			elif keyword == currentKeyWord:
				outputFile.write( '        <item>"' + components[index].strip() + '"</item>\n' )

	if needsClosure:
		outputFile.write( '    </string-array>\n' )					
	outputFile.write( '</resources>' )
 
	inputFile.close()
	outputFile.close()

# get languages from the first line of the file
inputFileName = "../translations.csv"
inputFile = open(inputFileName, 'r')
line = inputFile.readline()
inputFile.close()

components = line.split(',')

for i in range(1,components.__len__()):    
	component = components[i].split('::')
	appName = component[0]
	language = component[1].strip()
	print( "processing language: " + language )
	translate( appName, language, i )
