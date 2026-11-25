-- disco: turns the ground under you into a light-up dance floor for ~20 seconds, with a disco ball,
-- spinning light beams and an Am-F-C-G arpeggio on note blocks. Puts everything back when it's done.
-- If it gets killed partway, raise total_script_allowance in config.yml (try 5000).

local RADIUS = 4          -- floor is (2 * RADIUS + 1) blocks wide
local BALL_HEIGHT = 6     -- disco ball height above the floor
local STEP = 3            -- ticks per eighth note
local LOOPS = 4           -- times through the chord progression

local FLOOR = {
  "red_concrete", "orange_concrete", "yellow_concrete", "lime_concrete",
  "light_blue_concrete", "blue_concrete", "purple_concrete", "magenta_concrete",
}
local BEAMS = { "#ff2d95", "#2de1ff", "#fff12d", "#7dff2d" }

-- note block pitches: n = 0..24 semitones up from F#
local function pitch(n) return 2 ^ ((n - 12) / 12) end
local CHORDS = {                                -- 8 eighth notes each, up and back down
  { root = 3,  notes = { 3, 6, 10, 15, 18, 15, 10, 6 } },    -- Am
  { root = 11, notes = { 11, 15, 18, 23, 18, 15, 11, 15 } }, -- F
  { root = 6,  notes = { 6, 10, 13, 18, 22, 18, 13, 10 } },  -- C
  { root = 1,  notes = { 1, 5, 8, 13, 17, 13, 8, 5 } },      -- G
}

-- blocks with inventories or other data we can't put back, leave those alone
local SKIP = { "chest", "barrel", "shulker", "furnace", "hopper", "sign", "bed", "door", "spawner", "lectern" }
local function skippable(name)
  for _, s in ipairs(SKIP) do
    if string.find(name, s, 1, true) then return true end
  end
  return false
end

-- where the party is
local px, py, pz = position()
local cx, fy, cz = math.floor(px), math.floor(py) - 1, math.floor(pz)
local ballY = fy + BALL_HEIGHT

-- remember everything we're about to touch
local saved = {}
local function save(x, y, z)
  local name = block(x, y, z)
  if name and not skippable(name) then
    saved[#saved + 1] = { x = x, y = y, z = z, name = name }
    return true
  end
  return false
end

local tiles = {}
for dx = -RADIUS, RADIUS do
  for dz = -RADIUS, RADIUS do
    if save(cx + dx, fy, cz + dz) then
      tiles[#tiles + 1] = { x = cx + dx, z = cz + dz, ring = math.max(math.abs(dx), math.abs(dz)) }
    end
  end
end
local hasBall = save(cx, ballY, cz)
local oldTime, oldWeather = time(), weather()

local function restore()
  for i = #saved, 1, -1 do
    local b = saved[i]
    setBlock(b.x, b.y, b.z, b.name)
  end
  time(oldTime)
  weather(oldWeather)
end

local function paintFloor(beat)
  for _, t in ipairs(tiles) do
    -- rings ripple outward from the middle, checkerboard flips every beat
    local checker = (t.x + t.z + beat) % 2
    local color = FLOOR[(t.ring + beat + checker * 4) % #FLOOR + 1]
    setBlock(t.x, fy, t.z, color)
  end
end

local function beams(angle, colorShift)
  for i = 1, #BEAMS do
    local a = angle + (i - 1) * (math.pi * 2 / #BEAMS)
    local color = BEAMS[(i + colorShift) % #BEAMS + 1]
    for d = 1, 6 do
      particle("dust", cx + 0.5 + math.cos(a) * d, ballY + 0.5 - d * 0.8, cz + 0.5 + math.sin(a) * d,
        1, 0, 0, { color = color, size = 1.5 })
    end
  end
end

local function party()
  time(18000)
  weather("clear")
  if hasBall then setBlock(cx, ballY, cz, "sea_lantern") end

  title("<gradient:#ff2d95:#2de1ff><bold>DISCO</bold></gradient>", "<gray>dance floor deployed</gray>")
  sound("block.beacon.activate", 1.5)
  sleep(20)

  local step, beat, angle = 0, 0, 0
  for loop = 1, LOOPS do
    for c, chord in ipairs(CHORDS) do
      -- confetti and a little hop on every chord change
      particle("firework", cx + 0.5, ballY, cz + 0.5, 40, 1.5, 0.15)
      sound("entity.firework_rocket.twinkle", 1.2, 0.6)
      launch(0, 0.45, 0, true)

      for i, n in ipairs(chord.notes) do
        sound("block.note_block.harp", pitch(n), 0.8)
        particle("note", px, py + 2.3, pz, 1)

        if i % 2 == 1 then                                    -- on the beat
          sound("block.note_block.bass", pitch(chord.root), 1)
          if i % 4 == 1 then
            sound("block.note_block.basedrum", 1, 0.8)
          else
            sound("block.note_block.snare", 1, 0.6)
          end
          paintFloor(beat)
          beat = beat + 1
        else
          sound("block.note_block.hat", 1.4, 0.4)
        end

        beams(angle, beat)
        particle("end_rod", cx + 0.5, ballY + 0.5, cz + 0.5, 3, 0.6, 0.02)
        angle = angle + 0.35
        step = step + 1
        actionbar("<light_purple>[" .. ("|"):rep(step % 32) .. "</light_purple><dark_gray>" .. ("|"):rep(31 - step % 32) .. "]</dark_gray>")
        sleep(STEP)
      end
    end
  end

  -- big finish
  sound("block.note_block.bell", pitch(15), 1)
  sound("block.note_block.chime", pitch(22), 1)
  particle("totem_of_undying", cx + 0.5, fy + 2, cz + 0.5, 200, 2, 0.6)
  title("<gold><bold>thanks for dancing</bold></gold>", "<gray>the floor will now return to normal</gray>")
  sleep(40)
end

-- clean up even if something breaks halfway through
local ok, err = pcall(party)
restore()
sound("block.beacon.deactivate", 1)
if not ok then error(err, 0) end
