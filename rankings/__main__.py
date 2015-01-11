import urllib2
import os
import sys
from itertools import chain
from bs4 import BeautifulSoup
from datetime import datetime

SESSION_ID = open("rankings/sessionid", 'r').read() # Put your session ID here.

RANKING_RESULT_NAME = "rankings/rankings.txt"
REPLAYS_FOLDER_NAME = 'rankings'
RANKING_CACHE_NAME = "rankings/cache.txt"

REPLAYS_TEMPLATE = REPLAYS_FOLDER_NAME + "/%d.gz"
GUNZIP_TEMPLATE = REPLAYS_FOLDER_NAME + "/%d.gz"
REPLAY_FILE_TEMPLATE = REPLAYS_FOLDER_NAME + "/%d"

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
		date = datetime(int(split_date[2]), int(split_date[0]), int(split_date[1]), hour=int(split_time[0]), minute=int(split_time[1]))

	url = urllib2.build_opener()
	url.addheaders.append(('Cookie', 'sessionid=%s' % SESSION_ID))
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
	replays_needed = []

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
				is_opponent_A = False
			else:
				otherteam = win.contents[5].find('a').text.strip()
				is_opponent_A = True
			if otherteam not in teams:
				teams[otherteam] = dict()
			maps = "#%s: %s" % (num, win.contents[13].text.replace(u'\u200b', ''))
			if "wins" not in teams[otherteam]:
				teams[otherteam]["wins"] = [maps]
			else:
				teams[otherteam]["wins"].append(maps)
			replays_needed.append((int(num), otherteam, is_opponent_A))

		loss1 = soup.find_all("tr", class_="loss-1")
		loss2 = soup.find_all("tr", class_="loss-2")
		for loss in chain(loss1, loss2):
			num = loss.contents[15].find('a')['href'].split('/')[-1]
			loss_date = convert_date_time(loss.contents[3].text)
			if loss_date < date:
				continue
			if loss.contents[5].find('a')['href'][-3:] == '/96':
				otherteam = loss.contents[9].find('a').text.strip()
				is_opponent_A = False
			else:
				otherteam = loss.contents[5].find('a').text.strip()
				is_opponent_A = True
			if otherteam not in teams:
				teams[otherteam] = dict()
			maps = "#%s: %s" % (num, loss.contents[13].text.replace(u'\u200b', ''))
			if "losses" not in teams[otherteam]:
				teams[otherteam]["losses"] = [maps]
			else:
				teams[otherteam]["losses"].append(maps)
			replays_needed.append((int(num), otherteam, is_opponent_A))
	list_of_teams = sorted(teams.items(), key=calculateRatio)

	first_template = "%s: W/L Ratio: %s\n"
	second_template = "    %s:\n"
	third_template = "        %s\n"

	first_map_template = "%s - Map Ratios\n"
	second_map_template = "    %s: %d wins, %d losses (%d%%)\n"

	printout = "     Rankings since %s\n" % date.strftime("%B %d,%Y %H:%M (GMT)")

	printout += "=====================================================\n"
	printout += "            Per Team Aggregate Results\n"
	printout += "=====================================================\n"
	printout + "\n"

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

	printout += '\n'

	printout += "=====================================================\n"
	printout += "                Per Team Map Results\n"
	printout += "=====================================================\n"
	printout + "\n"

	team_info,map_results = calculate_per_map_results(replays_needed)
	for team in team_info.keys():
		printout += first_map_template % team
		list_of_maps = sorted(team_info[team].items(), key=calculatePercentage)
		for map_wrapper in list_of_maps:
			map_name = map_wrapper[0]
			wins = map_wrapper[1][0]
			losses = map_wrapper[1][1]
			printout += second_map_template % (map_name, wins, losses, calculatePercentage(map_wrapper))

	printout += '\n'

	printout += "=====================================================\n"
	printout += "               Aggregate Map Results\n"
	printout += "=====================================================\n"
	printout + "\n"

	third_map_template = "%s: %d wins, %d losses (%d%%)\n"

	list_of_map_results = sorted(map_results.items(), key=calculatePercentage)
	for mapz in list_of_map_results:
		map_name = mapz[0]
		map_results = mapz[1]
		wins = map_results[0]
		losses = map_results[1]
		printout += third_map_template % (map_name, wins, losses, calculatePercentage(mapz))

	print printout

	f = open(RANKING_RESULT_NAME, 'w')
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

def calculatePercentage(maps):
	if maps[1][0] == 0:
		return 0
	elif maps[1][1] == 0:
		return 100
	else:
		return int(float(maps[1][0])/(maps[1][0]+maps[1][1])*100)

def convert_date_time(string):
	split_string = string.strip().split(" ")
	split_date = split_string[0].split('/')
	split_time = split_string[1].split(':')
	hour = int(split_time[0])
	if(split_string[2] == 'PM'):
		hour += 12
	if hour == 24:
		hour = 23
	elif hour == 12:
		hour = 0
	return datetime(2000 + int(split_date[2]), int(split_date[0]), int(split_date[1]), 
		hour, int(split_time[1]))

def download_replay(replay_id):
	f = get_url("http://www.battlecode.org/scrimmage/download/%d" % replay_id)
	if f:
		replay = open(REPLAYS_TEMPLATE % replay_id, 'wb')
		replay.write(f.read())
		replay.close()
		
		os.system('gunzip -f %s' % (REPLAYS_TEMPLATE % replay_id))

		replay_file = open(REPLAY_FILE_TEMPLATE % replay_id, 'r')
		replay_info = replay_file.read()
		replay_file.close()

		return_list = []
		while True:
			map_index = replay_info.find("mapName=\"") + 9
			if map_index == 8:
				break
			map_name = ""
			while replay_info[map_index] != '"':
				map_name += replay_info[map_index]
				map_index += 1
			winner = replay_info.find("<ser.MatchFooter winner=\"")
			winner = replay_info[winner + 25]
			return_list.append((map_name, winner))
			replay_info = replay_info[replay_info.find("</ser.MatchFooter>") + 18: -1]

		os.remove(REPLAY_FILE_TEMPLATE % replay_id)
	return return_list

def get_url(link):
	try:
		url = urllib2.Request(link)
		url.add_header('Cookie', 'sessionid=%s' % SESSION_ID)
		f = urllib2.urlopen(url)
		return f
	except:
		return None

CACHE_LINE_TEMPLATE_2 = "%s,%s,%s|%s,%s|%s\n"
CACHE_LINE_TEMPLATE = "%s,%s,%s|%s,%s|%s,%s|%s\n"

def cache_result(replay_dict):
	cache_file = open(RANKING_CACHE_NAME, 'a')
	replay_id = replay_dict["id"]
	opponent = replay_dict["opponent"]
	map1,winner1 = replay_dict["map_1"]
	map2,winner2 = replay_dict["map_2"]
	if "map_3" in replay_dict:
		map3,winner3 = replay_dict["map_3"]
		cache_file.write(CACHE_LINE_TEMPLATE % (replay_id, opponent, map1, winner1, map2, winner2, map3, winner3))
	else:
		cache_file.write(CACHE_LINE_TEMPLATE_2 % (replay_id, opponent, map1, winner1, map2, winner2))
	cache_file.close()

def read_cache():
	try:
		cache_file = open(RANKING_CACHE_NAME, 'r+')
	except:
		return (set(), dict())
	saved_replays = cache_file.read().split('\n')
	cached_info = dict()
	for replay in saved_replays:
		if replay == '':
			continue
		replay_info = replay.split(',')
		replay_id = int(replay_info[0])
		opponent = replay_info[1]
		info_dict = dict()
		info_dict["opponent"] = opponent
		info_dict["maps"] = []
		for i in range(2, len(replay_info)):
			map_name, winner = replay_info[i].split('|')
			info_dict["maps"].append((map_name, winner))
		cached_info[replay_id] = info_dict
	return cached_info

def calculate_per_map_results(needed_replays):
	cached = read_cache()
	teams = dict()
	map_results = dict()
	for replay in needed_replays:
		replay_id = replay[0]
		opponent = replay[1]
		is_opponent_A = replay[2]
		if opponent not in teams:
			teams[opponent] = dict()
		if replay_id in cached:
			cached_replay = cached[replay_id]
			for map_name in cached_replay["maps"]:
				if map_name[0] not in teams[opponent]:
					teams[opponent][map_name[0]] = [0,0]
				if map_name[0] not in map_results:
					map_results[map_name[0]] = [0,0]
				if map_name[1] == "1":
					teams[opponent][map_name[0]][0] += 1
					map_results[map_name[0]][0] += 1
				else:
					teams[opponent][map_name[0]][1] += 1
					map_results[map_name[0]][1] += 1

		else:
			replay_info = download_replay(replay_id)
			cache_dictionary = dict()
			cache_dictionary["opponent"] = opponent
			cache_dictionary["id"] = replay_id
			for i in range(0, len(replay_info)):
				won = "1"
				if is_opponent_A == True and replay_info[i][1] == "A":
					won = "0"
				elif is_opponent_A == False and replay_info[i][1] == "B":
					won = "0"
				map_name = replay_info[i][0]
				cache_dictionary["map_%d"% (i+1)] = (map_name, won)
				if map_name not in teams[opponent]:
					teams[opponent][map_name] = [0,0]
				if map_name not in map_results:
					map_results[map_name] = [0,0]
				if won == "1":
					teams[opponent][map_name][0] += 1
					map_results[map_name][0] += 1
				else:
					teams[opponent][map_name][1] += 1
					map_results[map_name][1] += 1
			cache_result(cache_dictionary)
	return (teams, map_results)

main()