import operator

f = open('/Users/ebhalla/triangle.txt')
rows = []
line = f.readline()
while line:
    spaceValues = line.split('\n')[0].split(' ')
    for index, value in enumerate(spaceValues):
        spaceValues[index] = int(spaceValues[index])
    rows.append(spaceValues)
    line = f.readline()

for rowindex, row in enumerate(rows):
    dictPyramid[rowindex] = {}
    for index, item in enumerate(rows[rowindex]):
        dictPyramid[rowindex][index] = rows[rowindex][index]

print(dictPyramid[10])

for index, row in enumerate(dictPyramid):
    sortedPyramid[index] = sorted(dictPyramid[index].items(), key=operator.itemgetter(1), reverse=True)

def isLeftBetter(row, index):
	left = rows[row+1][index]
	right = rows[row+1][index+1]
	addToLeft = 0
	addToRight = 0
	if row+2 < len(rows):
		addToLeft = max(rows[row+2][index], rows[row+2][index+1])
		addToRight = max(rows[row+2][index+1], rows[row+2][index+2])
	return left+addToLeft > right+addToRight

f.close()