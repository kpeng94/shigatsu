import sys
import shutil
import itertools
import os

def main():
	if len(sys.argv) < 3:
		print "Not enough teams"
		sys.exit(0)

	teams = []
	for team in sys.argv[1:]:
		teams.append(team)

	shutil.move('matchmaker/matches', 'matchmaker/old_matches')
	f = open('matchmaker/matches', 'w')
	for match in itertools.permutations(teams, 2):
		f.write('%s|%s|*\n' % match)
	f.close()

	os.system('python matchmaker')

	shutil.move('matchmaker/old_matches', 'matchmaker/matches')

main()