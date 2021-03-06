#
# Copyright (c) 2009 Mason Green & Tom Novelli
#
# This file is part of OpenMelee.
#
# OpenMelee is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# any later version.
#
# OpenMelee is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with OpenMelee.  If not, see <http://www.gnu.org/licenses/>.
#
##
## KEYBOARD CONFIG   
##

from utils.key_map import *

KEYS = [
        # [thrust, left, right, fire, special],
        [K_W, K_A, K_D, K_PERIOD, K_SLASH],
        [K_UP, K_LEFT, K_RIGHT, K_LSHIFT, K_LCTRL],
]

##
## PLAYER CONFIG
##
from players.kbd import KeyboardPlayer, update_ship
from players.net import NetConn, NetPlayer
from players.cpu import CPU

PLAYERS = [KeyboardPlayer, CPU]

##
## SHIP SELECTION
##
from actors.ships.kzerZa import KzerZa
from actors.ships.nemesis import Nemesis

SHIP_CLASSES = KzerZa, Nemesis
SHIP_CHOICES = 0, 1

##
##
##
MAX_LAG = 500
