import urllib2
import sys
from itertools import chain
from bs4 import BeautifulSoup
from datetime import datetime

filename = "rankings/rankings.txt"

def main():
	date = datetime(2000, 1, 1)
	if(len(sys.argv) > 3):
		print "Too many arguments"
		sys.exit(0)
	if(len(sys.argv) == 2 and sys.argv[1] == "help"):
		print "syntax: python rankings [date] [time]"
		print "date syntax: MM/DD/YYYY"
		print "time syntax: HH:MM (24hr GMT)"
		sys.exit(0)
	if(len(sys.argv) == 2):
		split_date = sys.argv[1].split('/')
		date = datetime(int(split_date[2]), int(split_date[0]), int(split_date[1]))
	if(len(sys.argv) == 3):
		split_date = sys.argv[1].split('/')
		split_time = sys.argv[2].split(':')
		date = datetime(int(split_date[2]), int(split_date[0]), int(split_date[1]), int(split_time[0]), int(split_time[1]))

	url = urllib2.build_opener()
	url.addheaders.append(('Cookie', 'sessionid=put_your_session_id_here')) # SESSION ID HERE
 	url.addheaders.append(('Content-Type', 'text/html; charset=utf-8'))
	f = url.open("http://www.battlecode.org/scrimmage/matches/page/0")
	soup = BeautifulSoup(f.read().decode('utf-8'))
	numPages = 1

	### Determines the number of pages there are
	h3 = soup.find_all("h3");
	for heading in h3:
		text = heading.text
		if text.find("Page") >= 0:
			numPages = int(text[text.find("of") + 2 : text.find("(PREV")].strip())
	
	teams = dict()

	for i in range(0, numPages):
		### Find the wins
		f = url.open("http://www.battlecode.org/scrimmage/matches/page/%d" % i)
		soup = BeautifulSoup(f.read().decode('utf-8'))
		win1 = soup.find_all("tr", class_="win-1")
		win2 = soup.find_all("tr", class_="win-2")
		for win in chain(win1, win2):
			num = win.contents[15].find('a')['href'].split('/')[-1]
			win_date = convert_date_time(win.contents[3].text)
			if win_date < date:
				continue
			if win.contents[5].find('a')['href'][-3:] == '/96':
				otherteam = win.contents[9].find('a').text.strip()
			else:
				otherteam = win.contents[5].find('a').text.strip()
			if otherteam not in teams:
				teams[otherteam] = dict()
			maps = "#%s: %s" % (num, win.contents[13].text.replace(u'\u200b', ''))
			if "wins" not in teams[otherteam]:
				teams[otherteam]["wins"] = [maps]
			else:
				teams[otherteam]["wins"].append(maps)

		loss1 = soup.find_all("tr", class_="loss-1")
		loss2 = soup.find_all("tr", class_="loss-2")
		for loss in chain(loss1, loss2):
			num = loss.contents[15].find('a')['href'].split('/')[-1]
			loss_date = convert_date_time(loss.contents[3].text)
			if loss_date < date:
				continue
			if loss.contents[5].find('a')['href'][-3:] == '/96':
				otherteam = loss.contents[9].find('a').text.strip()
			else:
				otherteam = loss.contents[5].find('a').text.strip()
			if otherteam not in teams:
				teams[otherteam] = dict()
			maps = "#%s: %s" % (num, loss.contents[13].text.replace(u'\u200b', ''))
			if "losses" not in teams[otherteam]:
				teams[otherteam]["losses"] = [maps]
			else:
				teams[otherteam]["losses"].append(maps)

	list_of_teams = sorted(teams.items(), key=calculateRatio)

	first_template = "%s: W/L Ratio: %s\n"
	second_template = "    %s:\n"
	third_template = "        %s\n"

	printout = ""

	for team in list_of_teams:
		printout += first_template % (team[0], calculateWL(team))
		if "wins" in team[1]:
			printout += second_template % "Wins"
			for win in team[1]["wins"]:
				printout += third_template % win
		if "losses" in team[1]:
			printout += second_template % "Losses"
			for loss in team[1]["losses"]:
				printout += third_template % loss

	print printout
	f = open(filename, 'w')
	f.write(printout)
	f.close()

def calculateRatio(team):
	if "losses" not in team[1]:
		return len(team[1]["wins"])
	elif "wins" not in team[1]:
		return -100000 - len(team[1]["losses"])
	else:
		return -float(len(team[1]["losses"]))/len(team[1]["wins"])

def calculateWL(team):
	if "losses" not in team[1]:
		return "unbeaten"
	elif "wins" not in team[1]:
		return str(0)
	else:
		return str(float(len(team[1]["wins"]))/len(team[1]["losses"]))

def convert_date_time(string):
	split_string = string.strip().split(" ")
	split_date = split_string[0].split('/')
	split_time = split_string[1].split(':')
	hour = int(split_time[0])
	if(split_string[2] == 'PM'):
		hour += 12
	return datetime(2000 + int(split_date[2]), int(split_date[0]), int(split_date[1]), 
		hour, int(split_time[1]))

main()