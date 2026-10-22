"""Decide at which levels each skill rank goes.

PCGen spends skill points level by level: a level has its own points, and by
level n a skill holds at most n ranks. Ranks are given back from the highest
levels and placed from the lowest, the skills needing most ranks served first.
"""

from __future__ import annotations


def plan_ranks(budgets: list[int], placed: dict[str, list[int]], wanted: dict[str, int]) -> list[dict]:
    """The investments that move `placed` towards `wanted`, given each level's points left.

    `placed` maps a skill to the level indices that hold its ranks, and is updated in place.
    """
    budgets = list(budgets)
    investments = []
    for skill, ranks in wanted.items():
        levels = placed.setdefault(skill, [])
        levels.sort()
        while len(levels) > ranks:
            level = levels.pop()
            budgets[level] += 1
            investments.append({'skill_key': skill, 'points': -1, 'level_index': level})
    for level in range(len(budgets)):
        while budgets[level] > 0:
            candidates = [skill for skill, ranks in wanted.items()
                          if len(placed[skill]) < ranks and sum(1 for held in placed[skill] if held <= level) < level + 1]
            if not candidates:
                break
            skill = max(candidates, key=lambda name: (wanted[name] - len(placed[name]), wanted[name]))
            placed[skill].append(level)
            budgets[level] -= 1
            investments.append({'skill_key': skill, 'points': 1, 'level_index': level})
    return investments


OBSESSIVE_BONUSES = {'Craft': 'Obsessive Craft Bonus', 'Profession': 'Obsessive Profession Bonus'}


def headband_skill(published: dict[str, int]) -> str | None:
    """The skill a headband of vast intelligence gives ranks in: the published skill needing the most ranks."""
    return max(published, key=published.get, default=None)


def obsessive_bonus(published: dict[str, int]) -> tuple[str, str] | None:
    """A gnome's +2 on one Craft or Profession: the published one with the highest total."""
    candidates = {skill: total for skill, total in published.items() if skill.split(' (')[0] in OBSESSIVE_BONUSES}
    if not candidates:
        return None
    skill = max(candidates, key=candidates.get)
    return OBSESSIVE_BONUSES[skill.split(' (')[0]], skill
