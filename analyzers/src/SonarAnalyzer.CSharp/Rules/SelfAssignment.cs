﻿/*
 * SonarAnalyzer for .NET
 * Copyright (C) 2015-2023 SonarSource SA
 * mailto: contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

namespace SonarAnalyzer.Rules.CSharp
{
    [DiagnosticAnalyzer(LanguageNames.CSharp)]
    public sealed class SelfAssignment : SelfAssignmentBase<SyntaxKind>
    {
        protected override ILanguageFacade<SyntaxKind> Language => CSharpFacade.Instance;

        protected override void Initialize(SonarAnalysisContext context) =>
            context.RegisterNodeAction(c =>
                {
                    var expression = (AssignmentExpressionSyntax)c.Node;

                    if (expression.Parent is InitializerExpressionSyntax)
                    {
                        return;
                    }

                    foreach (var assigment in expression.MapAssignmentArguments().Where(x => CSharpEquivalenceChecker.AreEquivalent(x.Left, x.Right)))
                    {
                        c.ReportIssue(Rule.CreateDiagnostic(c.Compilation, assigment.Left.GetLocation(), additionalLocations: new[] { assigment.Right.GetLocation() }, properties: null));
                    }
                },
                SyntaxKind.SimpleAssignmentExpression,
                SyntaxKindEx.CoalesceAssignmentExpression);
    }
}
